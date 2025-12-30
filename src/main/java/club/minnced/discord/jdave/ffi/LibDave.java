package club.minnced.discord.jdave.ffi;

import static java.lang.foreign.ValueLayout.*;

import club.minnced.discord.jdave.DaveLoggingSeverity;
import club.minnced.discord.jdave.utils.NativeLibraryLoader;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

public class LibDave {
    static final Linker LINKER = Linker.nativeLinker();
    static final SymbolLookup SYMBOL_LOOKUP;
    public static final MemoryLayout C_SIZE;

    static {
        SYMBOL_LOOKUP = NativeLibraryLoader.getSymbolLookup();
        C_SIZE = LINKER.canonicalLayouts().get("size_t");
    }

    private LibDave() {}

    static final Logger log = LoggerFactory.getLogger(LibDave.class);
    static final MethodHandle daveMaxSupportedProtocolVersion;
    static final MethodHandle daveSetLogSinkCallback;
    static final MethodHandle free;

    static {
        try {
            // uint16_t daveMaxSupportedProtocolVersion(void);
            daveMaxSupportedProtocolVersion = LINKER.downcallHandle(
                    SYMBOL_LOOKUP.find("daveMaxSupportedProtocolVersion").orElseThrow(),
                    FunctionDescriptor.of(JAVA_SHORT));

            // void daveSetLogSinkCallback(DAVELogSinkCallback callback);
            daveSetLogSinkCallback = LINKER.downcallHandle(
                    SYMBOL_LOOKUP.find("daveSetLogSinkCallback").orElseThrow(), FunctionDescriptor.ofVoid(ADDRESS));

            // void free(void*);
            free = LINKER.downcallHandle(SYMBOL_LOOKUP.find("free").orElseThrow(), FunctionDescriptor.ofVoid(ADDRESS));
        } catch (Throwable e) {
            throw new ExceptionInInitializerError(e);
        }

        setLogSinkCallback(Arena.global(), (severity, file, line, message) -> {
            Level level =
                    switch (severity) {
                        case UNKNOWN -> Level.INFO;
                        case VERBOSE -> Level.TRACE;
                        case INFO -> Level.INFO;
                        case WARNING -> Level.WARN;
                        case ERROR -> Level.ERROR;
                        case NONE -> Level.INFO;
                    };

            int pathSeparatorIndex = Math.max(file.lastIndexOf('/'), file.lastIndexOf('\\'));
            String fileName = file;
            if (pathSeparatorIndex >= 0) {
                fileName = file.substring(pathSeparatorIndex + 1);
            }

            log.atLevel(level).log("{}:{} {}", fileName, line, message);
        });
    }

    public static void free(@NonNull MemorySegment segment) {
        try {
            free.invoke(segment);
        } catch (Throwable e) {
            throw new LibDaveBindingException(e);
        }
    }

    public static long readSize(@NonNull MemorySegment segment) {
        if (C_SIZE.byteSize() == 4) {
            return segment.get(JAVA_INT, 0);
        } else {
            return segment.get(JAVA_LONG, 0);
        }
    }

    static long sizeToLong(@NonNull Object size) {
        return ((Number) size).longValue();
    }

    public static short getMaxSupportedProtocolVersion() {
        try {
            return (short) daveMaxSupportedProtocolVersion.invoke();
        } catch (Throwable e) {
            throw new LibDaveBindingException(e);
        }
    }

    public static void setLogSinkCallback(@NonNull Arena arena, @NonNull LogSinkCallback logSinkCallback) {
        LogSinkCallbackMapper upcallMapper = new LogSinkCallbackMapper(logSinkCallback);

        MemorySegment upcall = LINKER.upcallStub(
                upcallMapper.getMethodHandle(), FunctionDescriptor.ofVoid(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS), arena);

        try {
            daveSetLogSinkCallback.invoke(upcall);
        } catch (Throwable e) {
            free(upcall);
            throw new LibDaveBindingException(e);
        }
    }

    // typedef void (*DAVELogSinkCallback)(DAVELoggingSeverity severity,
    //                                    const char* file,
    //                                    int line,
    //                                    const char* message);
    public interface LogSinkCallback {
        void onLogSink(@NonNull DaveLoggingSeverity severity, @NonNull String file, int line, @NonNull String message);
    }

    private static class LogSinkCallbackMapper {
        private static final MethodType TYPE =
                MethodType.methodType(void.class, Integer.TYPE, MemorySegment.class, Integer.TYPE, MemorySegment.class);

        private final LogSinkCallback logSinkCallback;

        LogSinkCallbackMapper(@NonNull LogSinkCallback logSinkCallback) {
            this.logSinkCallback = logSinkCallback;
        }

        public void onCallback(int severity, @NonNull MemorySegment file, int line, @NonNull MemorySegment message) {
            DaveLoggingSeverity severityEnum =
                    switch (severity) {
                        case 0 -> DaveLoggingSeverity.VERBOSE;
                        case 1 -> DaveLoggingSeverity.INFO;
                        case 2 -> DaveLoggingSeverity.WARNING;
                        case 3 -> DaveLoggingSeverity.ERROR;
                        case 4 -> DaveLoggingSeverity.NONE;
                        default -> DaveLoggingSeverity.UNKNOWN;
                    };

            logSinkCallback.onLogSink(
                    severityEnum, NativeUtils.asJavaString(file), line, NativeUtils.asJavaString(message));
        }

        @NonNull
        MethodHandle getMethodHandle() {
            try {
                return MethodHandles.lookup().bind(this, "onCallback", TYPE);
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
