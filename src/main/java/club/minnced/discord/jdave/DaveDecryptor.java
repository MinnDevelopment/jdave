package club.minnced.discord.jdave;

import static club.minnced.discord.jdave.ffi.LibDave.*;

import club.minnced.discord.jdave.ffi.LibDaveDecryptorBinding;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import org.jspecify.annotations.NonNull;

public class DaveDecryptor implements AutoCloseable {
    private final Arena arena;
    private final MemorySegment decryptor;

    private DaveDecryptor(@NonNull Arena arena, @NonNull MemorySegment decryptor) {
        this.arena = arena;
        this.decryptor = decryptor;
    }

    public static DaveDecryptor create() {
        return new DaveDecryptor(Arena.ofConfined(), LibDaveDecryptorBinding.createDecryptor());
    }

    private void destroy() {
        LibDaveDecryptorBinding.destroyDecryptor(decryptor);
    }

    public void prepareTransition(@NonNull DaveSessionImpl session, long selfUserId, int protocolVersion) {
        boolean disabled = protocolVersion == DaveConstants.DISABLED_PROTOCOL_VERSION;
        LibDaveDecryptorBinding.transitionToPassthroughMode(decryptor, disabled);

        if (!disabled) {
            try (DaveKeyRatchet keyRatchet = DaveKeyRatchet.create(session, Long.toUnsignedString(selfUserId))) {
                updateKeyRatchet(keyRatchet);
            }
        }
    }

    private void updateKeyRatchet(@NonNull DaveKeyRatchet ratchet) {
        LibDaveDecryptorBinding.transitionToKeyRatchet(decryptor, ratchet.getMemorySegment());
    }

    public long getMaxPlaintextByteSize(@NonNull DaveMediaType mediaType, long frameSize) {
        return LibDaveDecryptorBinding.getMaxPlaintextByteSize(decryptor, mediaType, frameSize);
    }

    @NonNull
    public DaveDecryptResult decrypt(
            @NonNull DaveMediaType mediaType, @NonNull ByteBuffer encrypted, @NonNull ByteBuffer decrypted) {
        try (Arena local = Arena.ofConfined()) {
            MemorySegment bytesWrittenPtr = local.allocate(C_SIZE);
            int result = LibDaveDecryptorBinding.decrypt(
                    decryptor,
                    mediaType,
                    MemorySegment.ofBuffer(encrypted),
                    MemorySegment.ofBuffer(decrypted),
                    bytesWrittenPtr);

            long bytesWritten = readSize(bytesWrittenPtr);
            DaveDecryptResultType resultType = DaveDecryptResultType.fromRaw(result);
            if (resultType == DaveDecryptResultType.SUCCESS && bytesWritten > 0) {
                decrypted.limit(decrypted.position() + (int) bytesWritten);
            }

            return new DaveDecryptResult(resultType, bytesWritten);
        }
    }

    @Override
    public void close() {
        destroy();
        arena.close();
    }

    public record DaveDecryptResult(@NonNull DaveDecryptResultType type, long bytesWritten) {}

    public enum DaveDecryptResultType {
        SUCCESS,
        FAILURE,
        ;

        @NonNull
        public static DaveDecryptResultType fromRaw(int type) {
            return switch (type) {
                case 0 -> SUCCESS;
                default -> FAILURE;
            };
        }
    }
}
