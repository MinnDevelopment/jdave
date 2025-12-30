package club.minnced.discord.jdave.manager;

import static club.minnced.discord.jdave.DaveConstants.MLS_NEW_GROUP_EXPECTED_EPOCH;

import club.minnced.discord.jdave.*;
import club.minnced.discord.jdave.ffi.LibDave;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.LongStream;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DaveSessionManager implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(DaveSessionManager.class);

    private final long selfUserId;
    private final long channelId;

    private final DaveSessionManagerCallbacks callbacks;
    private final DaveSessionImpl session;
    private final DaveEncryptor encryptor;
    private final Map<Long, DaveDecryptor> decryptors = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> preparedTransitions = new ConcurrentHashMap<>();

    private DaveSessionManager(long selfUserId, long channelId, @NonNull DaveSessionManagerCallbacks callbacks) {
        this(selfUserId, channelId, callbacks, DaveSessionImpl.create(null));
    }

    private DaveSessionManager(
            long selfUserId,
            long channelId,
            @NonNull DaveSessionManagerCallbacks callbacks,
            @NonNull DaveSessionImpl session) {
        this.selfUserId = selfUserId;
        this.channelId = channelId;
        this.callbacks = callbacks;
        this.session = session;
        this.encryptor = DaveEncryptor.create(session);
    }

    @NonNull
    public static DaveSessionManager create(
            long selfUserId, long channelId, @NonNull DaveSessionManagerCallbacks callbacks) {
        return new DaveSessionManager(selfUserId, channelId, callbacks);
    }

    @NonNull
    public static DaveSessionManager create(
            long selfUserId,
            long channelId,
            @NonNull DaveSessionManagerCallbacks callbacks,
            @Nullable String authSessionId) {
        return new DaveSessionManager(selfUserId, channelId, callbacks, DaveSessionImpl.create(authSessionId));
    }

    @Override
    public void close() {
        encryptor.close();
        decryptors.values().forEach(DaveDecryptor::close);
        decryptors.clear();
        session.close();
    }

    public int getMaxProtocolVersion() {
        return LibDave.getMaxSupportedProtocolVersion();
    }

    public void assignSsrcToCodec(@NonNull DaveCodec codec, int ssrc) {
        encryptor.assignSsrcToCodec(codec, ssrc);
    }

    public int getMaxEncryptedFrameSize(@NonNull DaveMediaType type, int frameSize) {
        return (int) encryptor.getMaxCiphertextByteSize(type, frameSize);
    }

    public int getMaxDecryptedFrameSize(@NonNull DaveMediaType type, long userId, int frameSize) {
        DaveDecryptor decryptor = this.decryptors.get(userId);
        if (decryptor == null) {
            return frameSize;
        }

        return (int) decryptor.getMaxPlaintextByteSize(type, frameSize);
    }

    public void encrypt(
            @NonNull DaveMediaType type, int ssrc, @NonNull ByteBuffer audio, @NonNull ByteBuffer encrypted) {
        encryptor.encrypt(type, ssrc, audio, encrypted);
    }

    public void decrypt(
            @NonNull DaveMediaType type, long userId, @NonNull ByteBuffer encrypted, @NonNull ByteBuffer decrypted) {
        DaveDecryptor decryptor = decryptors.get(userId);

        if (decryptor != null) {
            decryptor.decrypt(type, encrypted, decrypted);
        } else {
            // We don't know this user yet, so we assume it is passthrough
            decrypted.put(encrypted);
            decrypted.flip();
        }
    }

    public void addUser(long userId) {
        log.debug("Adding user {}", userId);
        decryptors.put(userId, DaveDecryptor.create());
    }

    public void removeUser(long userId) {
        log.debug("Removing user {}", userId);
        DaveDecryptor decryptor = decryptors.remove(userId);
        if (decryptor != null) {
            decryptor.close();
        }
    }

    public void onSelectProtocolAck(int protocolVersion) {
        log.debug("Handle select protocol version {}", protocolVersion);
        handleDaveProtocolInit(protocolVersion);
    }

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

    public void onDaveProtocolExecuteTransition(int transitionId) {
        log.debug("Handle dave protocol execute transition transitionId={}", transitionId);
        executeProtocolTransition(transitionId);
    }

    public void onDaveProtocolPrepareEpoch(@NonNull String epoch, int protocolVersion) {
        log.debug("Handle dave protocol prepare epoch epoch={} protocolVersion={}", epoch, protocolVersion);
        handlePrepareEpoch(epoch, (short) protocolVersion);
    }

    public void onDaveProtocolMLSExternalSenderPackage(@NonNull ByteBuffer externalSenderPackage) {
        log.debug("Handling external sender package");
        session.setExternalSender(externalSenderPackage);
    }

    public void onMLSProposals(@NonNull ByteBuffer proposals) {
        log.debug("Handling MLS proposals");
        session.processProposals(proposals, getUserIds(), callbacks::sendMLSCommitWelcome);
    }

    public void onMLSPrepareCommitTransition(int transitionId, @NonNull ByteBuffer commit) {
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

    public void onMLSWelcome(int transitionId, @NonNull ByteBuffer welcome) {
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

    @NonNull
    private List<@NonNull String> getUserIds() {
        return LongStream.concat(
                        LongStream.of(selfUserId), decryptors.keySet().stream().mapToLong(id -> id))
                .mapToObj(Long::toUnsignedString)
                .toList();
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

    private void handlePrepareEpoch(@NonNull String epoch, int protocolVersion) {
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
            encryptor.prepareTransition(selfUserId, protocolVersion);
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
