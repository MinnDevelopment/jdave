package club.minnced.discord.jdave;

import org.jspecify.annotations.NonNull;

// typedef enum { DAVE_MEDIA_TYPE_AUDIO = 0, DAVE_MEDIA_TYPE_VIDEO = 1 } DAVEMediaType;
public enum DaveMediaType {
    AUDIO,
    VIDEO,
    UNKNOWN,
    ;

    @NonNull
    public static DaveMediaType fromRaw(int type) {
        return switch (type) {
            case 0 -> DaveMediaType.AUDIO;
            case 1 -> DaveMediaType.VIDEO;
            default -> DaveMediaType.UNKNOWN;
        };
    }
}
