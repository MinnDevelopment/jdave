rootProject.name = "jdave"

includeBuild("native/libdave")

includeBuild("api/JDA") {
    dependencySubstitution { substitute(module("net.dv8tion:JDA")).using(project(":")) }
}

include("api", "native")