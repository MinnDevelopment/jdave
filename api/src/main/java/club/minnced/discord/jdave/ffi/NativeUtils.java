package club.minnced.discord.jdave.ffi;

import java.lang.foreign.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class NativeUtils {
    @NonNull
    public static String asJavaString(@NonNull MemorySegment nullTerminatedString) {
        return nullTerminatedString
                .reinterpret(12288) // 12K
                .getString(0);
    }

    public static boolean isNull(@Nullable MemorySegment segment) {
        return segment == null || MemorySegment.NULL.equals(segment);
    }

    static Object toSizeT(long number) {
        return LibDave.C_SIZE.byteSize() == 8 ? number : (int) number;
    }

    static long sizeToLong(@NonNull Object size) {
        return ((Number) size).longValue();
    }
}
