package club.minnced.discord.jdave;

import static club.minnced.discord.jdave.ffi.NativeUtils.isNull;

import club.minnced.discord.jdave.ffi.LibDaveKeyRatchetBinding;
import java.lang.foreign.MemorySegment;

public class DaveKeyRatchet implements AutoCloseable {
    private final MemorySegment keyRatchet;

    public DaveKeyRatchet(MemorySegment keyRatchet) {
        this.keyRatchet = keyRatchet;
    }

    public static DaveKeyRatchet create(DaveSessionImpl session, String selfUserId) {
        return new DaveKeyRatchet(session.getKeyRatchet(selfUserId));
    }

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
