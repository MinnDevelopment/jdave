package club.minnced.discord.jdave.ffi;

import club.minnced.discord.jdave.utils.NativeLibraryLoader;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

public class LibDaveLookup {
    static final Linker LINKER = Linker.nativeLinker();
    static final SymbolLookup SYMBOL_LOOKUP;
    public static final MemoryLayout C_SIZE;

    static {
        SYMBOL_LOOKUP = NativeLibraryLoader.getSymbolLookup();
        C_SIZE = LINKER.canonicalLayouts().get("size_t");
    }

    public static MethodHandle findVoid(String name, MemoryLayout... args) {
        return LINKER.downcallHandle(SYMBOL_LOOKUP.findOrThrow(name), FunctionDescriptor.ofVoid(args));
    }

    public static MethodHandle find(MemoryLayout returnType, String name, MemoryLayout... args) {
        return LINKER.downcallHandle(SYMBOL_LOOKUP.findOrThrow(name), FunctionDescriptor.of(returnType, args));
    }
}
