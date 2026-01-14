package club.minnced.discord.jdave.manager;

import static club.minnced.discord.jdave.DaveConstants.DISABLED_PROTOCOL_VERSION;
import static club.minnced.discord.jdave.DaveConstants.MLS_NEW_GROUP_EXPECTED_EPOCH;

import club.minnced.discord.jdave.*;
import club.minnced.discord.jdave.DaveDecryptor.DaveDecryptResultType;
import club.minnced.discord.jdave.DaveEncryptor.DaveEncryptResultType;
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

    private int currentProtocolVersion = DISABLED_PROTOCOL_VERSION;
    private volatile boolean shutdown = false;

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
        this.encryptor = DaveEncryptor.create(session, selfUserId);
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
    public synchronized void close() {
        shutdown = true;
        encryptor.close();
        decryptors.values().forEach(DaveDecryptor::close);
        decryptors.clear();
        session.close();
    }

    public int getMaxProtocolVersion() {
        return LibDave.getMaxSupportedProtocolVersion();
    }

    public synchronized void assignSsrcToCodec(@NonNull DaveCodec codec, int ssrc) {
        if (shutdown) {
            return;
        }

        encryptor.assignSsrcToCodec(codec, ssrc);
    }

    public synchronized int getMaxEncryptedFrameSize(@NonNull DaveMediaType type, int frameSize) {
        if (shutdown) {
            return frameSize;
        }

        return (int) encryptor.getMaxCiphertextByteSize(type, frameSize);
    }

    public synchronized int getMaxDecryptedFrameSize(@NonNull DaveMediaType type, long userId, int frameSize) {
        if (shutdown) {
            return frameSize;
        }

        DaveDecryptor decryptor = this.decryptors.get(userId);
        if (decryptor == null) {
            return frameSize;
        }

        return (int) decryptor.getMaxPlaintextByteSize(type, frameSize);
    }

    @NonNull
    public synchronized DaveEncryptResultType encrypt(
            @NonNull DaveMediaType type, int ssrc, @NonNull ByteBuffer audio, @NonNull ByteBuffer encrypted) {
        if (shutdown) {
            return DaveEncryptResultType.FAILURE;
        }

        DaveEncryptor.DaveEncryptorResult result = encryptor.encrypt(type, ssrc, audio, encrypted);
        return result.type();
    }

    @NonNull
    public synchronized DaveDecryptResultType decrypt(
            @NonNull DaveMediaType type, long userId, @NonNull ByteBuffer encrypted, @NonNull ByteBuffer decrypted) {
        if (shutdown) {
            return DaveDecryptResultType.FAILURE;
        }

        DaveDecryptor decryptor = decryptors.get(userId);

        if (decryptor != null) {
            return decryptor.decrypt(type, encrypted, decrypted).type();
        } else {
            return DaveDecryptResultType.FAILURE;
        }
    }

    @SuppressWarnings("resource")
    public synchronized void addUser(long userId) {
        if (shutdown) {
            return;
        }

        log.debug("Adding user {}", userId);
        DaveDecryptor decryptor = decryptors.computeIfAbsent(userId, id -> DaveDecryptor.create(id, session));
        decryptor.prepareTransition(currentProtocolVersion);
    }

    public synchronized void removeUser(long userId) {
        if (shutdown) {
            return;
        }

        log.debug("Removing user {}", userId);
        DaveDecryptor decryptor = decryptors.remove(userId);
        if (decryptor != null) {
            decryptor.close();
        }
    }

    public synchronized void onSelectProtocolAck(int protocolVersion) {
        if (shutdown) {
            return;
        }

        log.debug("Handle select protocol version {}", protocolVersion);
        handleDaveProtocolInit(protocolVersion);
    }

    public synchronized void onDaveProtocolPrepareTransition(int transitionId, int protocolVersion) {
        if (shutdown) {
            return;
        }

        log.debug(
                "Handle dave protocol prepare transition transitionId={} protocolVersion={}",
                transitionId,
                protocolVersion);

        prepareProtocolTransition(transitionId, protocolVersion);
    }

    public synchronized void onDaveProtocolExecuteTransition(int transitionId) {
        if (shutdown) {
            return;
        }

        log.debug("Handle dave protocol execute transition transitionId={}", transitionId);
        executeProtocolTransition(transitionId);
    }

    public synchronized void onDaveProtocolPrepareEpoch(long epoch, int protocolVersion) {
        if (shutdown) {
            return;
        }

        log.debug("Handle dave protocol prepare epoch epoch={} protocolVersion={}", epoch, protocolVersion);
        handlePrepareEpoch(epoch, (short) protocolVersion);
    }

    public synchronized void onDaveProtocolMLSExternalSenderPackage(@NonNull ByteBuffer externalSenderPackage) {
        if (shutdown) {
            return;
        }

        log.debug("Handling external sender package");
        session.setExternalSender(externalSenderPackage);
    }

    public synchronized void onMLSProposals(@NonNull ByteBuffer proposals) {
        if (shutdown) {
            return;
        }

        log.debug("Handling MLS proposals");
        session.processProposals(proposals, getRecognizedUserIds(), callbacks::sendMLSCommitWelcome);
    }

    public synchronized void onMLSPrepareCommitTransition(int transitionId, @NonNull ByteBuffer commit) {
        if (shutdown) {
            return;
        }

        log.debug("Handling MLS prepare commit transition transitionId={}", transitionId);
        DaveSessionImpl.CommitResult result = session.processCommit(commit);
        switch (result) {
            case DaveSessionImpl.CommitResult.Ignored ignored -> {
                preparedTransitions.remove(transitionId);
            }
            case DaveSessionImpl.CommitResult.Success success -> {
                if (success.joined()) {
                    prepareProtocolTransition(transitionId, session.getProtocolVersion());
                } else {
                    sendInvalidCommitWelcome(transitionId);
                    handleDaveProtocolInit(transitionId);
                }
            }
        }
    }

    public synchronized void onMLSWelcome(int transitionId, @NonNull ByteBuffer welcome) {
        if (shutdown) {
            return;
        }

        log.debug("Handling MLS welcome transition transitionId={}", transitionId);
        boolean joinedGroup = session.processWelcome(welcome, getRecognizedUserIds());

        if (joinedGroup) {
            prepareProtocolTransition(transitionId, session.getProtocolVersion());
        } else {
            sendInvalidCommitWelcome(transitionId);
            handleDaveProtocolInit(transitionId);
        }
    }

    @NonNull
    private List<@NonNull String> getRecognizedUserIds() {
        return LongStream.concat(
                        LongStream.of(selfUserId), decryptors.keySet().stream().mapToLong(id -> id))
                .mapToObj(Long::toUnsignedString)
                .toList();
    }

    private void handleDaveProtocolInit(int protocolVersion) {
        log.debug("Initializing dave protocol session for protocol version {}", protocolVersion);
        if (protocolVersion > DaveConstants.DISABLED_PROTOCOL_VERSION) {
            handlePrepareEpoch(MLS_NEW_GROUP_EXPECTED_EPOCH, protocolVersion);
        } else {
            prepareProtocolTransition(DaveConstants.INIT_TRANSITION_ID, protocolVersion);
            executeProtocolTransition(DaveConstants.INIT_TRANSITION_ID);
        }
    }

    private void handlePrepareEpoch(long epoch, int protocolVersion) {
        if (epoch != MLS_NEW_GROUP_EXPECTED_EPOCH) {
            return;
        }

        session.initialize((short) protocolVersion, channelId, Long.toUnsignedString(selfUserId));
        session.sendMarshalledKeyPackage(callbacks::sendMLSKeyPackage);
    }

    private void prepareProtocolTransition(int transitionId, int protocolVersion) {
        log.debug("Preparing to transition to protocol version={} (Transition ID {})", protocolVersion, transitionId);
        decryptors.forEach((userId, decryptor) -> {
            if (userId == selfUserId) {
                return;
            }

            decryptor.prepareTransition(protocolVersion);
        });

        if (transitionId == DaveConstants.INIT_TRANSITION_ID) {
            encryptor.processTransition(protocolVersion);
        } else {
            preparedTransitions.put(transitionId, protocolVersion);
            currentProtocolVersion = protocolVersion;
            callbacks.sendDaveProtocolReadyForTransition(transitionId);
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
