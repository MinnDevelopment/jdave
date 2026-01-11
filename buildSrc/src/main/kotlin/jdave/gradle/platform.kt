package jdave.gradle

import org.gradle.api.Project
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

fun Project.getPlatform(triplet: String = targetPlatform) = Platform.parse(triplet)

val Project.targetPlatform: String
    get() = findProperty("target") as? String ?: detectPlatformTriplet()

fun detectPlatformTriplet(): String {
    val os = with(DefaultNativePlatform.getCurrentOperatingSystem()) {
        when {
            isMacOsX -> "darwin"
            isWindows -> "windows"
            isLinux -> "linux"
            else -> error("Unknown operating system: $this")
        }
    }

    val arch = with(DefaultNativePlatform.getCurrentArchitecture()) {
        when {
            isAmd64 -> "x86_64"
            isArm64 -> "aarch64"
            isArm -> "arm"
            else -> error("Unknown architecture: $this")
        }
    }

    return "$arch-unknown-$os"
}

data class Platform(val operatingSystem: OperatingSystem, val arch: Architecture, val musl: Boolean) {
    companion object {
        fun parse(triplet: String): Platform {
            val normalized = triplet.lowercase()
            return Platform(
                operatingSystem = OperatingSystem.parse(normalized),
                arch = Architecture.parse(normalized),
                musl = "musl" in normalized
            )
        }
    }

    override fun toString(): String =
        listOfNotNull(operatingSystem.key, if (musl) "musl" else null, arch.key).joinToString("-")
}

enum class OperatingSystem(val key: String, val libraryPattern: Regex) {
    Linux("linux", Regex("lib(\\w+)\\.so")),
    MacOS("darwin", Regex("lib(\\w+)\\.dylib")),
    Windows("win", Regex("(\\w+)\\.dll"));

    companion object {
        fun parse(triplet: String) = when {
            "linux" in triplet -> Linux
            "darwin" in triplet -> MacOS
            "windows" in triplet -> Windows
            else -> error("Unknown operating system: $triplet")
        }
    }
}

enum class Architecture(val key: String?) {
    X86("x86"),
    X86_64("x86-64"),
    AARCH64("aarch64"),
    ARM("arm"),
    DARWIN(null);

    companion object {
        fun parse(triplet: String) = when {
            "x86_64" in triplet -> X86_64
            listOf("x86", "i386", "i486", "i586", "i686").any { it in triplet } -> X86
            "aarch64" in triplet -> AARCH64
            "arm" in triplet -> ARM
            "darwin" in triplet -> DARWIN
            else -> error("Unknown architecture: $triplet")
        }
    }
}
