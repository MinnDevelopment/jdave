package club.minnced.discord.jdave;

import static club.minnced.discord.jdave.ffi.LibDave.C_SIZE;
import static club.minnced.discord.jdave.ffi.LibDave.readSize;

import club.minnced.discord.jdave.ffi.LibDaveEncryptorBinding;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DaveEncryptor implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(DaveEncryptor.class);
    private final MemorySegment encryptor;
    private final DaveSessionImpl session;
    private final long selfUserId;

    private DaveEncryptor(@NonNull MemorySegment encryptor, @NonNull DaveSessionImpl session, long selfUserId) {
        this.encryptor = encryptor;
        this.session = session;
        this.selfUserId = selfUserId;

        LibDaveEncryptorBinding.setPassthroughMode(encryptor, true);
    }

    @NonNull
    public static DaveEncryptor create(DaveSessionImpl session, long selfUserId) {
        return new DaveEncryptor(LibDaveEncryptorBinding.createEncryptor(), session, selfUserId);
    }

    private void destroy() {
        LibDaveEncryptorBinding.destroyEncryptor(encryptor);
    }

    public void prepareTransition(int protocolVersion) {
        log.debug("Preparing to transition to protocol version {}", protocolVersion);
        boolean disabled = protocolVersion == DaveConstants.DISABLED_PROTOCOL_VERSION;

        if (!disabled) {
            updateKeyRatchet();
        }
    }

    public void processTransition(int protocolVersion) {
        log.debug("Transitioning to protocol version {}", protocolVersion);
        boolean disabled = protocolVersion == DaveConstants.DISABLED_PROTOCOL_VERSION;

        if (!disabled) {
            updateKeyRatchet();
        }

        LibDaveEncryptorBinding.setPassthroughMode(encryptor, disabled);
    }

    private void updateKeyRatchet() {
        try (DaveKeyRatchet keyRatchet = DaveKeyRatchet.create(session, Long.toUnsignedString(selfUserId))) {
            log.debug("Updating key ratchet");
            LibDaveEncryptorBinding.setKeyRatchet(encryptor, keyRatchet.getMemorySegment());
        }
    }

    public long getMaxCiphertextByteSize(@NonNull DaveMediaType mediaType, long frameSize) {
        return LibDaveEncryptorBinding.getMaxCiphertextByteSize(encryptor, mediaType.ordinal(), frameSize);
    }

    public void assignSsrcToCodec(@NonNull DaveCodec codec, int ssrc) {
        LibDaveEncryptorBinding.assignSsrcToCodec(encryptor, ssrc, codec.ordinal());
    }

    @NonNull
    public DaveEncryptorResult encrypt(
            @NonNull DaveMediaType mediaType, int ssrc, @NonNull ByteBuffer input, @NonNull ByteBuffer output) {
        try (Arena local = Arena.ofConfined()) {
            MemorySegment bytesWrittenPtr = local.allocate(C_SIZE);

            int result = LibDaveEncryptorBinding.encrypt(
                    encryptor,
                    mediaType.ordinal(),
                    ssrc,
                    MemorySegment.ofBuffer(input),
                    MemorySegment.ofBuffer(output),
                    bytesWrittenPtr);

            long bytesWritten = readSize(bytesWrittenPtr);
            DaveEncryptResultType resultType = DaveEncryptResultType.fromRaw(result);

            if (resultType == DaveEncryptResultType.SUCCESS && bytesWritten > 0) {
                output.limit(output.position() + (int) bytesWritten);
            }

            return new DaveEncryptorResult(resultType, bytesWritten);
        }
    }

    @Override
    public void close() {
        this.destroy();
    }

    public record DaveEncryptorResult(@NonNull DaveEncryptResultType type, long bytesWritten) {}

    public enum DaveEncryptResultType {
        SUCCESS,
        FAILURE,
        ;

        @NonNull
        public static DaveEncryptResultType fromRaw(int result) {
            return switch (result) {
                case 0 -> SUCCESS;
                default -> FAILURE;
            };
        }
    }
}
