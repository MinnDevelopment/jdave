package club.minnced.discord.jdave;

import static club.minnced.discord.jdave.ffi.LibDave.*;

import club.minnced.discord.jdave.ffi.LibDaveDecryptorBinding;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;

public class DaveDecryptor implements AutoCloseable {
    private final Arena arena;
    private final MemorySegment decryptor;

    private DaveDecryptor(Arena arena, MemorySegment decryptor) {
        this.arena = arena;
        this.decryptor = decryptor;
    }

    public static DaveDecryptor create() {
        return new DaveDecryptor(Arena.ofConfined(), LibDaveDecryptorBinding.createDecryptor());
    }

    private void destroy() {
        LibDaveDecryptorBinding.destroyDecryptor(decryptor);
    }

    public void prepareTransition(DaveSessionImpl session, long selfUserId, int protocolVersion) {
        boolean disabled = protocolVersion == DaveConstants.DISABLED_PROTOCOL_VERSION;
        LibDaveDecryptorBinding.transitionToPassthroughMode(decryptor, disabled);

        if (!disabled) {
            try (DaveKeyRatchet keyRatchet = DaveKeyRatchet.create(session, Long.toUnsignedString(selfUserId))) {
                updateKeyRatchet(keyRatchet);
            }
        }
    }

    private void updateKeyRatchet(DaveKeyRatchet ratchet) {
        LibDaveDecryptorBinding.transitionToKeyRatchet(decryptor, ratchet.getMemorySegment());
    }

    public long getMaxPlaintextByteSize(DaveMediaType mediaType, long frameSize) {
        return LibDaveDecryptorBinding.getMaxPlaintextByteSize(decryptor, mediaType, frameSize);
    }

    public DaveDecryptResult decrypt(DaveMediaType mediaType, ByteBuffer encrypted, ByteBuffer decrypted) {
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

    public record DaveDecryptResult(DaveDecryptResultType type, long bytesWritten) {}

    public enum DaveDecryptResultType {
        SUCCESS,
        FAILURE,
        ;

        public static DaveDecryptResultType fromRaw(int type) {
            return switch (type) {
                case 0 -> SUCCESS;
                default -> FAILURE;
            };
        }
    }
}
