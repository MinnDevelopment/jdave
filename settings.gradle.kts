rootProject.name = "jdave"

includeBuild("libdave")

includeBuild("JDA") {
    dependencySubstitution { substitute(module("net.dv8tion:JDA")).using(project(":")) }
}
