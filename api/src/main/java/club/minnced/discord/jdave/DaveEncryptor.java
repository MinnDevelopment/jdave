package club.minnced.discord.jdave;

import static club.minnced.discord.jdave.ffi.LibDave.C_SIZE;
import static club.minnced.discord.jdave.ffi.LibDave.readSize;

import club.minnced.discord.jdave.ffi.LibDaveEncryptorBinding;
import club.minnced.discord.jdave.ffi.NativeUtils;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import org.jspecify.annotations.NonNull;

public class DaveEncryptor implements AutoCloseable {
    private final MemorySegment encryptor;
    private final DaveSessionImpl session;

    private DaveEncryptor(@NonNull MemorySegment encryptor, @NonNull DaveSessionImpl session) {
        this.encryptor = encryptor;
        this.session = session;

        LibDaveEncryptorBinding.setPassthroughMode(encryptor, true);
    }

    @NonNull
    public static DaveEncryptor create(DaveSessionImpl session) {
        return new DaveEncryptor(LibDaveEncryptorBinding.createEncryptor(), session);
    }

    private void destroy() {
        LibDaveEncryptorBinding.destroyEncryptor(encryptor);
    }

    public boolean prepareTransition(long selfUserId, int protocolVersion) {
        boolean disabled = protocolVersion == DaveConstants.DISABLED_PROTOCOL_VERSION;

        if (!disabled) {
            try (DaveKeyRatchet keyRatchet = DaveKeyRatchet.create(session, Long.toUnsignedString(selfUserId))) {
                LibDaveEncryptorBinding.setKeyRatchet(encryptor, keyRatchet.getMemorySegment());
                return !NativeUtils.isNull(keyRatchet.getMemorySegment());
            }
        }
        return false;
    }

    public void processTransition(int protocolVersion) {
        boolean disabled = protocolVersion == DaveConstants.DISABLED_PROTOCOL_VERSION;
        LibDaveEncryptorBinding.setPassthroughMode(encryptor, disabled);
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
