[![.github/workflows/build.yml](https://github.com/MinnDevelopment/jdave/actions/workflows/build.yml/badge.svg)](https://github.com/MinnDevelopment/jdave/actions/workflows/build.yml)

# Java Discord Audio & Video Encryption (JDAVE)

This library provides java bindings for [libdave](https://github.com/discord/libdave).

To implement these bindings, this library uses Java **Foreign Function & Memory API** (FFM).

## Requirements

- Java 25

## Supported Platforms

API:

[![](https://img.shields.io/maven-central/v/club.minnced/jdave-api?color=blue&label=jdave-api)](https://search.maven.org/artifact/club.minnced/jdave-api)

Linux:

[![](https://img.shields.io/maven-central/v/club.minnced/jdave-native-linux-x86-64?color=blue&label=linux-x86-64&logo=linux&logoColor=white)](https://search.maven.org/artifact/club.minnced/jdave-native-linux-x86-64)

Windows:

[![](https://img.shields.io/maven-central/v/club.minnced/jdave-native-win-x86-64?color=blue&label=win-x86-64)](https://search.maven.org/artifact/club.minnced/jdave-native-win-x86-64)

## Installation

The coordinates for the dependency include the target platform, currently only one is supported.

```gradle
repositories {
    mavenCentral()
}

dependencies {
    // Interface to use for libraries
    implementation("club.minnced:jdave-api:0.1.0-rc.1")

    // Compiled natives for libdave for the specified platform
    implementation("club.minnced:jdave-native-linux-x86-64:0.1.0-rc.1")
    implementation("club.minnced:jdave-native-windows-x86-64:0.1.0-rc.1")
}
```

## Example: JDA

To use this library with [JDA](https://github.com/discord-jda/JDA), you can use the [JDaveSessionFactory](api/src/main/java/club/minnced/discord/jdave/interop/JDaveSessionFactory.java) to configure the audio module:

```java
JDABuilder.createLight(TOKEN)
  .setAudioModuleConfig(new AudioModuleConfig()
    .withDaveSessionFactory(new JDaveSessionFactory()))
  .build()
```

## Restricted Methods Warning

When you use this library, you will receive warnings due to usage of [restricted methods](https://docs.oracle.com/en/java/javase/25/core/restricted-methods.html) like this:

```
WARNING: A restricted method in java.lang.foreign.SymbolLookup has been called
WARNING: java.lang.foreign.SymbolLookup::libraryLookup has been called by club.minnced.discord.jdave.utils.NativeLibraryLoader in an unnamed module (file:.../jdave-api-0.0.1.jar)
WARNING: Use --enable-native-access=ALL-UNNAMED to avoid a warning for callers in this module
WARNING: Restricted methods will be blocked in a future release unless native access is enabled
```

To remove these warnings, you need to either allow these methods through the command line arguments:

```shell
java --enable-native-access=ALL-UNNAMED ...
```

Or enabling them in your JAR-file manifest:

```shell
Enable-Native-Access: ALL-UNNAMED
```

## Why Java 25?

This library uses the [Foreign Function & Memory (FFM) API](https://docs.oracle.com/en/java/javase/22/core/foreign-function-and-memory-api.html) which has been stabilized in Java 22.
Since Java 22 is not a Long-Term-Support release, this library instead uses Java 25.

Other alternatives have downsides compared to the newer FFM API:

- JNI breaks relocation, since it requires the package to match the C symbols. To relocate a JNI native binding, you must rename the native symbols, which is quite cumbersome.
- JNI requires compiling the library for each platform, we can't just call C libraries directly without some C glue code that needs to be compiled for every target platform.
- JNA can be relocated and does not require extra C glue code, but has significant overhead, which is a problem in performance critical hot paths, like with audio encryption.
- JNA is also a massive library, which is a dependency we can avoid by using Java's standard library entirely.

For this reason, I've chosen to use the new FFM API instead. The optimal solution would be a full Java implementation, instead of relying on native libraries. But that requires a lot more time, which we don't have right now.
