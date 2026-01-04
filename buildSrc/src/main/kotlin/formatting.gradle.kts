import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    id("com.diffplug.spotless")
}

configure<SpotlessExtension> {
    kotlinGradle { ktfmt().kotlinlangStyle() }

    java {
        palantirJavaFormat("2.83.0")

        removeUnusedImports()
        trimTrailingWhitespace()
    }
}
