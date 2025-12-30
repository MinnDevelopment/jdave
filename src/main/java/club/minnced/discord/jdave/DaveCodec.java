package club.minnced.discord.jdave;

import org.jspecify.annotations.NonNull;

public enum DaveCodec {
    UNKNOWN,
    OPUS,
    VP8,
    VP9,
    H264,
    H265,
    AV1,
    ;

    @NonNull
    public static DaveCodec fromRaw(int codec) {
        return switch (codec) {
            case 1 -> OPUS;
            case 2 -> VP8;
            case 3 -> VP9;
            case 4 -> H264;
            case 5 -> H265;
            case 6 -> AV1;
            default -> UNKNOWN;
        };
    }
}
