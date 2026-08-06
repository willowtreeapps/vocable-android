plugins {
    id("vocable.library")
    alias(libs.plugins.kotlinAndroid)
}

android {
    namespace = "com.willowtree.vocable.basetest"
}

dependencies {
    implementation(project(":app"))
}