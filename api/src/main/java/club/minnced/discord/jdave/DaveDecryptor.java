package club.minnced.discord.jdave;

import static club.minnced.discord.jdave.ffi.LibDave.*;

import club.minnced.discord.jdave.ffi.LibDaveDecryptorBinding;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DaveDecryptor implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(DaveDecryptor.class);
    private final long userId;
    private final DaveSessionImpl session;
    private final MemorySegment decryptor;

    private DaveDecryptor(long userId, @NonNull DaveSessionImpl session, @NonNull MemorySegment decryptor) {
        this.userId = userId;
        this.session = session;
        this.decryptor = decryptor;
    }

    @NonNull
    public static DaveDecryptor create(long userId, @NonNull DaveSessionImpl session) {
        return new DaveDecryptor(userId, session, LibDaveDecryptorBinding.createDecryptor());
    }

    private void destroy() {
        LibDaveDecryptorBinding.destroyDecryptor(decryptor);
    }

    public void prepareTransition(int protocolVersion) {
        log.debug("Preparing transition to protocol version {}", protocolVersion);
        boolean disabled = protocolVersion == DaveConstants.DISABLED_PROTOCOL_VERSION;

        if (!disabled) {
            updateKeyRatchet();
        }

        LibDaveDecryptorBinding.transitionToPassthroughMode(decryptor, disabled);
    }

    private void updateKeyRatchet() {
        try (DaveKeyRatchet keyRatchet = DaveKeyRatchet.create(session, Long.toUnsignedString(userId))) {
            log.debug("Updating key ratchet");
            LibDaveDecryptorBinding.transitionToKeyRatchet(decryptor, keyRatchet.getMemorySegment());
        }
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
