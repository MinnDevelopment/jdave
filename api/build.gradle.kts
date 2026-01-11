plugins { `publishing-environment` }

publishingEnvironment { moduleName = "jdave-api" }

dependencies {
    compileOnly(libs.jspecify)
    implementation(libs.slf4j.api)

    compileOnly(libs.jda)
}
