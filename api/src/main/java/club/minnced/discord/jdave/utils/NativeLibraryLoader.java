package club.minnced.discord.jdave.utils;

import club.minnced.discord.jdave.ffi.LibDaveBindingException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.StringJoiner;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class NativeLibraryLoader {
    @NonNull
    public static NativeLibrary getNativeLibrary() {
        return resolveLibrary("dave");
    }

    @NonNull
    public static Path createTemporaryFile() {
        NativeLibrary nativeLibrary = getNativeLibrary();

        try (InputStream library = NativeLibraryLoader.class.getResourceAsStream(nativeLibrary.resourcePath())) {
            if (library == null) {
                throw new LibDaveBindingException(
                        "Could not find resource for current platform. Looked for " + nativeLibrary.resourcePath());
            }

            Path tempDirectory = Files.createTempDirectory("jdave");
            Path tempFile = Files.createTempFile(
                    tempDirectory,
                    nativeLibrary.libraryName(),
                    "." + nativeLibrary.os().getLibraryExtension());

            try (OutputStream outputStream = Files.newOutputStream(tempFile)) {
                library.transferTo(outputStream);
            }

            return tempFile;
        } catch (Throwable e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @NonNull
    public static SymbolLookup getSymbolLookup() {
        Path tempFile = createTemporaryFile();
        return SymbolLookup.libraryLookup(tempFile, Arena.global());
    }

    @NonNull
    public static NativeLibrary resolveLibrary(@NonNull String baseName) {
        return resolveLibrary(baseName, System.getProperty("os.name"), System.getProperty("os.arch"));
    }

    @NonNull
    public static NativeLibrary resolveLibrary(
            @NonNull String baseName, @NonNull String osName, @NonNull String archName) {
        OperatingSystem os = getOperatingSystem(osName);

        if (os == OperatingSystem.MACOS) {
            return new NativeLibrary(OperatingSystem.MACOS, Architecture.DARWIN, baseName);
        }

        Architecture arch = getArchitecture(archName);
        return new NativeLibrary(os, arch, baseName);
    }

    @NonNull
    public static OperatingSystem getOperatingSystem() {
        return getOperatingSystem(System.getProperty("os.name"));
    }

    @NonNull
    private static OperatingSystem getOperatingSystem(@NonNull String osName) {
        osName = osName.toLowerCase();
        if (osName.contains("linux")) {
            return OperatingSystem.LINUX;
        }
        if (osName.contains("mac") || osName.contains("darwin")) {
            return OperatingSystem.MACOS;
        }
        if (osName.contains("win")) {
            return OperatingSystem.WINDOWS;
        }
        throw new UnsupportedOperationException("Unsupported OS: " + osName);
    }

    @NonNull
    private static Architecture getArchitecture(@NonNull String arch) {
        arch = arch.toLowerCase();
        return switch (arch) {
            case "x86_64", "amd64" -> Architecture.X86_64;
            case "aarch64", "arm64" -> Architecture.AARCH64;
            case "x86", "i386", "i486", "i586", "i686" -> Architecture.X86;
            case "darwin" -> Architecture.DARWIN;
            default -> throw new UnsupportedOperationException("Unsupported arch: " + arch);
        };
    }

    public record NativeLibrary(
            @NonNull OperatingSystem os,
            @NonNull Architecture arch,
            @NonNull String libraryName) {
        public String resourcePath() {
            StringJoiner path = new StringJoiner("/");
            path.add("/natives");

            StringJoiner platform = new StringJoiner("-");
            platform.add(os.key);
            if (arch.key != null) {
                platform.add(arch.key);
            }

            path.add(platform.toString());
            path.add(os.getLibraryName(libraryName));

            return path.toString();
        }
    }

    public enum OperatingSystem {
        LINUX("linux", "lib", "so"),
        MACOS("darwin", "lib", "dylib"),
        WINDOWS("win", "", "dll"),
        ;

        private final String key;
        private final String libraryPrefix;
        private final String libraryExtension;

        OperatingSystem(String key, String libraryPrefix, String libraryExtension) {
            this.key = key;
            this.libraryPrefix = libraryPrefix;
            this.libraryExtension = libraryExtension;
        }

        @NonNull
        public String getKey() {
            return key;
        }

        @NonNull
        public String getLibraryPrefix() {
            return libraryPrefix;
        }

        @NonNull
        public String getLibraryExtension() {
            return libraryExtension;
        }

        @NonNull
        public String getLibraryName(@NonNull String name) {
            return String.format(Locale.ROOT, "%s%s.%s", libraryPrefix, name, libraryExtension);
        }
    }

    public enum Architecture {
        X86("x86"),
        X86_64("x86-64"),
        ARM("arm"),
        AARCH64("aarch64"),
        DARWIN(null),
        ;

        private final String key;

        Architecture(String key) {
            this.key = key;
        }

        @Nullable
        public String getKey() {
            return key;
        }
    }
}
