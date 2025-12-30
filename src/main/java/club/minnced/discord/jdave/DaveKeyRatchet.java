package club.minnced.discord.jdave;

import static club.minnced.discord.jdave.ffi.NativeUtils.isNull;

import club.minnced.discord.jdave.ffi.LibDaveKeyRatchetBinding;
import java.lang.foreign.MemorySegment;
import org.jspecify.annotations.NonNull;

public class DaveKeyRatchet implements AutoCloseable {
    private final MemorySegment keyRatchet;

    public DaveKeyRatchet(@NonNull MemorySegment keyRatchet) {
        this.keyRatchet = keyRatchet;
    }

    @NonNull
    public static DaveKeyRatchet create(@NonNull DaveSessionImpl session, @NonNull String selfUserId) {
        return new DaveKeyRatchet(session.getKeyRatchet(selfUserId));
    }

    @NonNull
    public MemorySegment getMemorySegment() {
        return keyRatchet;
    }

    @Override
    public void close() {
        if (!isNull(keyRatchet)) {
            LibDaveKeyRatchetBinding.destroyKeyRatchet(this.keyRatchet);
        }
    }
}
