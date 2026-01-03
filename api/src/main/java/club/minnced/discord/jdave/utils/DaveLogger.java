package club.minnced.discord.jdave.utils;

import club.minnced.discord.jdave.DaveLoggingSeverity;
import club.minnced.discord.jdave.ffi.LibDave;
import java.lang.foreign.Arena;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

public class DaveLogger {
    @SuppressWarnings("LoggerInitializedWithForeignClass")
    public static final Logger log = LoggerFactory.getLogger(LibDave.class);

    private static final Arena arena = Arena.global();

    public static void init() {
        LibDave.setLogSinkCallback(arena, DaveLogger::log);
    }

    private static void log(
            @NonNull DaveLoggingSeverity severity, @NonNull String file, int line, @NonNull Object message) {
        Level level = mapLogLevel(severity);

        int pathSeparatorIndex = Math.max(file.lastIndexOf('/'), file.lastIndexOf('\\'));
        String fileName = file;
        if (pathSeparatorIndex >= 0) {
            fileName = file.substring(pathSeparatorIndex + 1);
        }

        log.atLevel(level).log("{}:{} {}", fileName, line, message);
    }

    @NonNull
    private static Level mapLogLevel(@NonNull DaveLoggingSeverity severity) {
        return switch (severity) {
            case UNKNOWN -> Level.INFO;
            case VERBOSE -> Level.TRACE;
            case INFO -> Level.DEBUG; // libdave logs pretty much everything on INFO
            case WARNING -> Level.WARN;
            case ERROR -> Level.ERROR;
            case NONE -> Level.INFO;
        };
    }
}
