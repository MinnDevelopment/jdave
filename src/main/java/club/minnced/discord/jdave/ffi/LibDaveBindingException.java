package club.minnced.discord.jdave.ffi;

import org.jspecify.annotations.NonNull;

public class LibDaveBindingException extends RuntimeException {
    public LibDaveBindingException(@NonNull String message) {
        super(message);
    }

    public LibDaveBindingException(@NonNull Throwable cause) {
        super(cause);
    }
}
