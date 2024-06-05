plugins {
    id("vocable.library")
}

android {
    namespace = "com.willowtree.vocable.basetest"
}

dependencies {
    implementation(project(":app"))
}