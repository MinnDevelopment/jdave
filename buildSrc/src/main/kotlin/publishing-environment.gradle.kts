import org.gradle.kotlin.dsl.withType
import org.jreleaser.gradle.plugin.JReleaserExtension
import org.jreleaser.gradle.plugin.tasks.AbstractJReleaserTask
import org.jreleaser.model.Active

plugins {
    `java-library`
    `maven-publish`

    id("org.jreleaser")
}


interface PublishingEnvironmentExtension {
    val moduleName: Property<String>
}

val publishingEnvironment = project.extensions.create<PublishingEnvironmentExtension>("publishingEnvironment")

configure<JavaPluginExtension> {
    withJavadocJar()
    withSourcesJar()

    afterEvaluate {
        base.archivesName = publishingEnvironment.moduleName
    }
}

val stagingDirectory: Directory get() = layout.buildDirectory.dir("staging-deploy").get()

configure<PublishingExtension> {
    publications {
        register<MavenPublication>("Release") {
            from(components["java"])

            afterEvaluate {
                artifactId = publishingEnvironment.moduleName.get()
                pom.name.set(artifactId)
            }

            groupId = group.toString()
            version = version.toString()

            artifact(tasks.named("javadocJar"))
            artifact(tasks.named("sourcesJar"))

            pom.apply {
                packaging = "jar"
                description.set("Rust implementation of the JDA-NAS interface")
                url.set("https://github.com/MinnDevelopment/jdave")
                scm {
                    url.set("https://github.com/MinnDevelopment/jdave")
                    connection.set("scm:git:git://github.com/MinnDevelopment/jdave")
                    developerConnection.set("scm:git:ssh:git@github.com:MinnDevelopment/jdave")
                }
                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("Minn")
                        name.set("Florian Spieß")
                        email.set("business@minn.dev")
                    }
                }
            }
        }
    }

    repositories.maven {
        url = stagingDirectory.asFile.toURI()
    }
}

configure<JReleaserExtension> {
    gitRootSearch = true

    release {
        github {
            enabled = false
        }
    }

    signing.pgp {
        active = Active.RELEASE
        armored = true
    }

    deploy {
        maven {
            mavenCentral {
                register("sonatype") {
                    active = Active.RELEASE
                    url = "https://central.sonatype.com/api/v1/publisher"
                    stagingRepository(stagingDirectory.asFile.relativeTo(projectDir).path)
                }
            }
        }
    }
}

tasks.withType<AbstractJReleaserTask>().configureEach {
    mustRunAfter(tasks.named("publish"))
}