plugins {
    `kotlin-dsl`
}

group = "com.willowtree.vocable.buildlogic"

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
}