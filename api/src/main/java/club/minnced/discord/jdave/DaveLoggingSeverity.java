package club.minnced.discord.jdave;

import org.jspecify.annotations.NonNull;

public enum DaveLoggingSeverity {
    UNKNOWN,
    VERBOSE,
    INFO,
    WARNING,
    ERROR,
    NONE,
    ;

    @NonNull
    public static DaveLoggingSeverity fromRaw(int severity) {
        return switch (severity) {
            case 0 -> DaveLoggingSeverity.VERBOSE;
            case 1 -> DaveLoggingSeverity.INFO;
            case 2 -> DaveLoggingSeverity.WARNING;
            case 3 -> DaveLoggingSeverity.ERROR;
            case 4 -> DaveLoggingSeverity.NONE;
            default -> DaveLoggingSeverity.UNKNOWN;
        };
    }
}
