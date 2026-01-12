plugins {
    java

    alias(libs.plugins.version.catalog.update)
}

group = "club.minnced"
version = "0.1.2"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "formatting")

    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenLocal()
        mavenCentral()
    }

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25

        withJavadocJar()
        withSourcesJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release = 25

        options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-options", "-Xlint:-restricted"))
    }
}
