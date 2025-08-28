plugins {
    id("vocable.application")
    id("kotlin-kapt")
    id("kotlin-parcelize")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.willowtree.vocable"

    defaultConfig {
        targetSdk = 35
        applicationId = "com.willowtree.vocable"

        val versionCodeEnv = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
        versionCode = versionCodeEnv + 30

        versionName = System.getenv("VERSION_NAME") ?: "pre-release($versionCode)"

        testInstrumentationRunner = "com.willowtree.vocable.utility.VocableTestRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
            }
        }
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("keystore")
            storePassword = System.getenv("RELEASE_KEY_PASSWORD")
            keyAlias = System.getenv("RELEASE_KEY_ALIAS")
            keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            buildConfigField("boolean", "USE_HEAD_TRACKING", "true")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        getByName("debug") {
            val useHeadTracking = project.findProperty("USE_HEAD_TRACKING")?.toString() ?: "true"
            buildConfigField("boolean", "USE_HEAD_TRACKING", useHeadTracking)
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }

    sourceSets {
        getByName("androidTest") {
            assets.srcDirs("schemas")
        }
    }

    useLibrary("android.test.runner")
}

dependencies {
    implementation(libs.koin.android)
    implementation(libs.androidx.runtime)
    implementation(libs.androidx.ui)
    debugImplementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.turbine)

    // AndroidX
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.viewpager2)

    // Google AR
    implementation(libs.google.arcore)
    implementation(libs.google.sceneform)

    // Kotlin Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // View customization
    implementation(libs.inflationx.calligraphy)
    implementation(libs.inflationx.viewpump)

    // Android Arch (Legacy)
    implementation(libs.android.arch.lifecycle.extensions)
    implementation(libs.android.arch.lifecycle.viewmodel)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    debugImplementation(libs.androidx.ui.tooling)
    kapt(libs.androidx.room.compiler)

    // Security
    implementation(libs.androidx.security.crypto)

    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Logging & Analytics
    implementation(libs.timber)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics.ktx)

    // Moshi
    implementation(libs.moshi.kotlin)

    // Koin (DI)
    implementation(libs.koin.android)

    // Unit Tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.turbine)
    testImplementation(project(":basetest"))

    // Android Instrumentation Tests
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.espresso.intents)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.uiautomator)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.arch.core.testing)
    androidTestImplementation(libs.turbine)
    androidTestImplementation(project(":basetest"))
    androidTestImplementation(libs.androidx.navigation.testing)
}