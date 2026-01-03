import jdave.gradle.getPlatform

plugins { `publishing-environment` }

publishingEnvironment { moduleName = "jdave-native-${getPlatform()}" }

dependencies {
    api(project(":api"))

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("ch.qos.logback:logback-classic:1.5.23")
}

val nativeResourceRoot = "resources/libdave"

val assembleNatives by
    tasks.registering(Copy::class) {
        dependsOn(gradle.includedBuild("libdave").task(":cpp:cmakeAssemble"))

        from(project.layout.projectDirectory.dir("libdave/cpp/build/libs"))
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
