plugins { `publishing-environment` }

publishingEnvironment { moduleName = "jdave-api" }

dependencies {
    compileOnly(libs.jspecify)
    implementation(libs.slf4j.api)

    // TODO: Fix this version on proper release
    compileOnly(libs.jda)
}
