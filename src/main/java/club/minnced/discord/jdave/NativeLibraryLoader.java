package club.minnced.discord.jdave;

import club.minnced.discord.jdave.ffi.LibDaveBindingException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import java.nio.file.Files;
import java.nio.file.Path;

public class NativeLibraryLoader {
    public static NativeLibrary getNativeLibrary() {
        return resolveLibrary("dave");
    }

    public static Path createTemporaryFile() {
        NativeLibrary nativeLibrary = getNativeLibrary();

        try (InputStream library = NativeLibraryLoader.class.getResourceAsStream(nativeLibrary.resourcePath())) {
            if (library == null) {
                throw new LibDaveBindingException(
                        "Could not find resource for current platform. Looked for " + nativeLibrary.resourcePath());
            }

            Path tempDirectory = Files.createTempDirectory("jdave");
            Path tempFile =
                    Files.createTempFile(tempDirectory, nativeLibrary.libraryName(), "." + nativeLibrary.extension());

            try (OutputStream outputStream = Files.newOutputStream(tempFile)) {
                library.transferTo(outputStream);
            }

            return tempFile;
        } catch (Throwable e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static SymbolLookup getSymbolLookup() {
        Path tempFile = createTemporaryFile();
        return SymbolLookup.libraryLookup(tempFile, Arena.global());
    }

    public static NativeLibrary resolveLibrary(String baseName) {
        String os = normalizeOs(System.getProperty("os.name"));
        String arch = normalizeArch(System.getProperty("os.arch"));

        String platform = os + "-" + arch;
        String prefix = libraryPrefix(os);
        String extension = libraryExtension(os);

        return new NativeLibrary(platform, prefix + baseName, extension);
    }

    private static String normalizeOs(String osName) {
        osName = osName.toLowerCase();
        if (osName.contains("win")) {
            return "windows";
        }
        if (osName.contains("mac") || osName.contains("darwin")) {
            return "macos";
        }
        if (osName.contains("linux")) {
            return "linux";
        }
        throw new UnsupportedOperationException("Unsupported OS: " + osName);
    }

    private static String normalizeArch(String arch) {
        arch = arch.toLowerCase();
        return switch (arch) {
            case "x86_64", "amd64" -> "x86-64";
            case "aarch64", "arm64" -> "aarch64";
            case "x86", "i386", "i486", "i586", "i686" -> "x86";
            default -> throw new UnsupportedOperationException("Unsupported arch: " + arch);
        };
    }

    private static String libraryPrefix(String os) {
        return os.equals("windows") ? "" : "lib";
    }

    private static String libraryExtension(String os) {
        return switch (os) {
            case "windows" -> "dll";
            case "macos" -> "dylib";
            case "linux" -> "so";
            default -> throw new AssertionError(os);
        };
    }

    public record NativeLibrary(String platform, String libraryName, String extension) {
        public String resourcePath() {
            return "/natives/" + platform + "/" + libraryName + "." + extension;
        }
    }
}
