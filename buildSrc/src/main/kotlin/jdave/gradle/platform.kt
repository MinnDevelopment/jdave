package jdave.gradle

import org.gradle.api.Project
import java.util.StringJoiner

fun Project.getPlatform(triplet: String = targetPlatform) = Platform.parse(triplet)

val Project.targetPlatform: String get() = findProperty("target") as? String ?: "x86_64-unknown-linux-gnu"

data class Platform(
    val operatingSystem: OperatingSystem,
    val arch: Architecture,
    val musl: Boolean,
) {
    companion object {
        fun parse(triplet: String = "x86_64-unknown-linux-gnu"): Platform {
            return Platform(
                operatingSystem = OperatingSystem.parse(triplet),
                arch = Architecture.parse(triplet),
                musl = "musl" in triplet,
            )
        }
    }

    override fun toString(): String {
        val joiner = StringJoiner("-")
        joiner.add(operatingSystem.key)
        if (musl) {
            joiner.add("musl")
        }
        arch.key?.let(joiner::add)
        return joiner.toString()
    }
}

enum class OperatingSystem(val key: String, val libraryPattern: Regex) {
    Linux("linux", Regex("lib(\\w+)\\.so")),
    MacOS("darwin", Regex("lib(\\w+)\\.dylib")),
    Windows("win", Regex("(\\w+)\\.dll")),
    ;

    companion object {
        fun parse(triplet: String): OperatingSystem = when {
            "linux" in triplet -> Linux
            "darwin" in triplet -> MacOS
            "windows" in triplet -> Windows
            else -> throw IllegalArgumentException("Unknown operating system: $triplet")
        }
    }
}

enum class Architecture(val key: String?) {
    X86("x86"),
    X86_64("x86-64"),
    AARCH64("aarch64"),
    ARM("arm"),
    DARWIN(null),
    ;

    companion object {
        fun parse(triplet: String): Architecture = when {
            "x86" in triplet -> X86
            "x86_64" in triplet -> X86_64
            "aarch64" in triplet -> AARCH64
            "arm" in triplet -> ARM
            "darwin" in triplet -> DARWIN
            else -> throw IllegalArgumentException("Unknown arch: $triplet")
        }
    }
}