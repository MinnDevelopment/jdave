package club.minnced.discord.jdave.interop;

import static club.minnced.discord.jdave.DaveConstants.MLS_NEW_GROUP_EXPECTED_EPOCH;

import club.minnced.discord.jdave.*;
import club.minnced.discord.jdave.ffi.LibDave;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.dv8tion.jda.api.audio.dave.DaveProtocolCallbacks;
import net.dv8tion.jda.api.audio.dave.DaveSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JDaveSession implements DaveSession {
    private static final Logger log = LoggerFactory.getLogger(JDaveSession.class);

    private final long selfUserId;
    private final long channelId;

    private final DaveProtocolCallbacks callbacks;
    private final DaveSessionImpl session;
    private final DaveEncryptor encryptor;
    private final Map<Long, DaveDecryptor> decryptors = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> preparedTransitions = new ConcurrentHashMap<>();

    public JDaveSession(long selfUserId, long channelId, DaveProtocolCallbacks callbacks, DaveSessionImpl session) {
        this.selfUserId = selfUserId;
        this.channelId = channelId;
        this.callbacks = callbacks;
        this.session = session;
        this.encryptor = DaveEncryptor.create(session);
    }

    @Override
    public int getMaxProtocolVersion() {
        return LibDave.getMaxSupportedProtocolVersion();
    }

    @Override
    public void assignSsrcToCodec(Codec codec, int ssrc) {
        if (codec == Codec.OPUS) {
            encryptor.assignSsrcToCodec(DaveCodec.OPUS, ssrc);
        }
    }

    @Override
    public int getMaxEncryptedFrameSize(MediaType type, int frameSize) {
        if (type != MediaType.AUDIO) {
            return frameSize * 2;
        }

        return (int) encryptor.getMaxCiphertextByteSize(DaveMediaType.AUDIO, frameSize);
    }

    @Override
    public int getMaxDecryptedFrameSize(MediaType type, long userId, int frameSize) {
        DaveDecryptor decryptor = this.decryptors.get(userId);
        if (decryptor == null) {
            return frameSize;
        }

        if (type != MediaType.AUDIO) {
            return frameSize * 2;
        }

        return (int) decryptor.getMaxPlaintextByteSize(DaveMediaType.AUDIO, frameSize);
    }

    @Override
    public void encryptOpus(int ssrc, ByteBuffer audio, ByteBuffer encrypted) {
        encryptor.encrypt(DaveMediaType.AUDIO, ssrc, audio, encrypted);
    }

    @Override
    public void decryptOpus(long userId, ByteBuffer encrypted, ByteBuffer decrypted) {
        DaveDecryptor decryptor = decryptors.get(userId);

        if (decryptor != null) {
            decryptor.decrypt(DaveMediaType.AUDIO, encrypted, decrypted);
        } else {
            // We don't know this user yet, so we assume it is passthrough
            decrypted.put(encrypted);
            decrypted.flip();
        }
    }

    @Override
    public void addUser(long userId) {
        decryptors.put(userId, DaveDecryptor.create());
    }

    @Override
    public void removeUser(long userId) {
        DaveDecryptor decryptor = decryptors.remove(userId);
        if (decryptor != null) {
            decryptor.close();
        }
    }

    @Override
    public void initialize() {}

    @Override
    public void destroy() {
        encryptor.close();
        decryptors.values().forEach(DaveDecryptor::close);
        decryptors.clear();
        session.close();
    }

    @Override
    public void onSelectProtocolAck(int protocolVersion) {
        log.debug("Handle select protocol version {}", protocolVersion);
        handleDaveProtocolInit(protocolVersion);
    }

    @Override
    public void onDaveProtocolPrepareTransition(int transitionId, int protocolVersion) {
        log.debug(
                "Handle dave protocol prepare transition transitionId={} protocolVersion={}",
                transitionId,
                protocolVersion);

        prepareProtocolTransition(transitionId, protocolVersion);
        if (transitionId != DaveConstants.INIT_TRANSITION_ID) {
            callbacks.sendDaveProtocolReadyForTransition(transitionId);
        }
    }

    @Override
    public void onDaveProtocolExecuteTransition(int transitionId) {
        log.debug("Handle dave protocol execute transition transitionId={}", transitionId);
        executeProtocolTransition(transitionId);
    }

    @Override
    public void onDaveProtocolPrepareEpoch(String epoch, int protocolVersion) {
        log.debug("Handle dave protocol prepare epoch epoch={} protocolVersion={}", epoch, protocolVersion);
        handlePrepareEpoch(epoch, (short) protocolVersion);
    }

    @Override
    public void onDaveProtocolMLSExternalSenderPackage(ByteBuffer externalSenderPackage) {
        log.debug("Handling external sender package");
        session.setExternalSender(externalSenderPackage);
    }

    @Override
    public void onMLSProposals(ByteBuffer proposals) {
        log.debug("Handling MLS proposals");
        session.processProposals(proposals, getUserIds(), callbacks::sendMLSCommitWelcome);
    }

    @Override
    public void onMLSPrepareCommitTransition(int transitionId, ByteBuffer commit) {
        log.debug("Handling MLS prepare commit transition transitionId={}", transitionId);
        DaveSessionImpl.CommitResult result = session.processCommit(commit);
        switch (result) {
            case DaveSessionImpl.CommitResult.Ignored ignored -> {
                preparedTransitions.remove(transitionId);
            }
            case DaveSessionImpl.CommitResult.Success success -> {
                if (success.joined()) {
                    prepareProtocolTransition(transitionId, session.getProtocolVersion());
                    if (transitionId != DaveConstants.INIT_TRANSITION_ID) {
                        callbacks.sendDaveProtocolReadyForTransition(transitionId);
                    }
                } else {
                    sendInvalidCommitWelcome(transitionId);
                    handleDaveProtocolInit(transitionId);
                }
            }
        }
    }

    @Override
    public void onMLSWelcome(int transitionId, ByteBuffer welcome) {
        log.debug("Handling MLS welcome transition transitionId={}", transitionId);
        boolean joinedGroup = session.processWelcome(welcome, getUserIds());

        if (joinedGroup) {
            prepareProtocolTransition(transitionId, session.getProtocolVersion());
            if (transitionId != DaveConstants.INIT_TRANSITION_ID) {
                callbacks.sendDaveProtocolReadyForTransition(transitionId);
            }
        } else {
            sendInvalidCommitWelcome(transitionId);
            handleDaveProtocolInit(transitionId);
        }
    }

    private List<String> getUserIds() {
        return decryptors.keySet().stream().map(Long::toUnsignedString).toList();
    }

    private void handleDaveProtocolInit(int protocolVersion) {
        log.debug("Initializing dave protocol session for protocol version {}", protocolVersion);
        if (protocolVersion > DaveConstants.DISABLED_PROTOCOL_VERSION) {
            handlePrepareEpoch(MLS_NEW_GROUP_EXPECTED_EPOCH, protocolVersion);
            session.sendMarshalledKeyPackage(callbacks::sendMLSKeyPackage);
        } else {
            prepareProtocolTransition(DaveConstants.INIT_TRANSITION_ID, protocolVersion);
            executeProtocolTransition(DaveConstants.INIT_TRANSITION_ID);
        }
    }

    private void handlePrepareEpoch(String epoch, int protocolVersion) {
        if (!MLS_NEW_GROUP_EXPECTED_EPOCH.equals(epoch)) {
            return;
        }

        session.initialize((short) protocolVersion, channelId, Long.toUnsignedString(selfUserId));
    }

    private void prepareProtocolTransition(int transitionId, int protocolVersion) {
        log.debug("Preparing to transition to protocol version={} (Transition ID {})", protocolVersion, transitionId);
        decryptors.forEach((userId, decryptor) -> {
            if (userId == selfUserId) {
                return;
            }

            decryptor.prepareTransition(session, selfUserId, protocolVersion);
        });

        if (transitionId == DaveConstants.INIT_TRANSITION_ID) {
            encryptor.prepareTransition(session, selfUserId, protocolVersion);
        } else {
            preparedTransitions.put(transitionId, protocolVersion);
        }
    }

    private void executeProtocolTransition(int transitionId) {
        Integer protocolVersion = preparedTransitions.remove(transitionId);
        if (protocolVersion == null) {
            log.warn("Unexpected Transition ID {}", transitionId);
            return;
        }

        log.debug("Executing transition to protocol version {} (Transition ID {})", protocolVersion, transitionId);

        if (protocolVersion == DaveConstants.DISABLED_PROTOCOL_VERSION) {
            session.reset();
        }

        encryptor.processTransition(protocolVersion);
    }

    private void sendInvalidCommitWelcome(int transitionId) {
        callbacks.sendMLSInvalidCommitWelcome(transitionId);
        session.sendMarshalledKeyPackage(callbacks::sendMLSKeyPackage);
    }
}
