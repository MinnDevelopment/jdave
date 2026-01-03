package club.minnced.discord.jdave.ffi;

import static club.minnced.discord.jdave.ffi.LibDave.LINKER;
import static club.minnced.discord.jdave.ffi.LibDave.SYMBOL_LOOKUP;
import static java.lang.foreign.ValueLayout.ADDRESS;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import org.jspecify.annotations.NonNull;

public class LibDaveKeyRatchetBinding {
    private static final MethodHandle destroyKeyRatchet;

    static {
        try {
            // void daveKeyRatchetDestroy(DAVEKeyRatchetHandle keyRatchet);
            destroyKeyRatchet = LINKER.downcallHandle(
                    SYMBOL_LOOKUP.find("daveKeyRatchetDestroy").orElseThrow(), FunctionDescriptor.ofVoid(ADDRESS));
        } catch (Throwable e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static void destroyKeyRatchet(@NonNull MemorySegment segment) {
        try {
            destroyKeyRatchet.invoke(segment);
        } catch (Throwable e) {
            throw new LibDaveBindingException(e);
        }
    }
}
