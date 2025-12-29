import jdave.gradle.getPlatform

plugins {
    `java-library`
    id("com.diffplug.spotless") version "8.1.0"
}

group = "club.minnced"

version = "1.0-SNAPSHOT"

val artifactName = "${project.name}-${getPlatform()}"

base { archivesName.set(artifactName) }

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.17")

    // TODO: Fix this version on proper release
    compileOnly("net.dv8tion:JDA:6.3.0")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("ch.qos.logback:logback-classic:1.5.23")
}

val nativeResourceRoot = "resources/libdave"

val assembleNatives by
    tasks.registering(Copy::class) {
        dependsOn(gradle.includedBuild("libdave").task(":cpp:cmakeAssemble"))

        from(layout.projectDirectory.dir("libdave/cpp/build/libs"))
        into(layout.buildDirectory.dir("$nativeResourceRoot/natives/${getPlatform()}"))
    }

tasks.processResources {
    dependsOn(assembleNatives)
    from(layout.buildDirectory.dir(nativeResourceRoot))
}

tasks.test {
    useJUnitPlatform()

    jvmArgs = listOf("--enable-native-access=ALL-UNNAMED")

    testLogging { events("passed", "skipped", "failed") }

    reports { html.required = true }
}

spotless {
    kotlinGradle { ktfmt().kotlinlangStyle() }

    java {
        palantirJavaFormat()

        removeUnusedImports()
        trimTrailingWhitespace()
    }
}
