plugins {
    id("vocable.application")
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.ksp)
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.willowtree.vocable"

    defaultConfig {
        targetSdk = 36
        applicationId = "com.willowtree.vocable"

        val versionCodeEnv = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
        versionCode = versionCodeEnv + 30

        versionName = System.getenv("VERSION_NAME") ?: "pre-release($versionCode)"

        testInstrumentationRunner = "com.willowtree.vocable.utility.VocableTestRunner"
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
            applicationIdSuffix = ".debug"
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }

    // MediaPipe .task bundles are themselves zip archives internally - if AAPT re-compresses
    // them when packaging assets, MediaPipe's direct byte-offset reads into the bundle break
    // and it silently fails to detect anything (no thrown error). The asset only exists in the
    // debug source set (#678 engine-comparison tooling); this setting is harmless for release.
    androidResources {
        noCompress += "task"
    }

    sourceSets {
        getByName("androidTest") {
            assets.directories.add("schemas")
        }
    }

    useLibrary("android.test.runner")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Room 2.8.4's room-migration JAR was compiled against an older kotlinx.serialization where
// typeParametersSerializers() in GeneratedSerializer had no implementation. In 1.7.3 it became
// abstract, causing AbstractMethodError at runtime. In 1.8.1 it is a JVM default method again,
// so Room's pre-compiled serializers work correctly. Force 1.8.1 to override Room's strict 1.7.3.
configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1")
    }
}

dependencies {
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.androidx.runtime)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.material3)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.turbine)

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.activity.compose)

    // SceneView
    implementation(libs.sceneview.arsceneview)

    // Debug-only tracking-engine comparison (#678): MediaPipe + CameraX live exclusively in
    // the debug source set, so these MUST stay debugImplementation - release artifacts carry
    // neither the libraries nor the model assets.
    debugImplementation(libs.androidx.camera.core)
    debugImplementation(libs.androidx.camera.camera2)
    debugImplementation(libs.androidx.camera.lifecycle)
    debugImplementation(libs.mediapipe.tasks.vision)

    // Kotlin Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    debugImplementation(libs.androidx.ui.tooling)
    ksp(libs.androidx.room.compiler)

    // Security
    implementation(libs.androidx.security.crypto)

    // Navigation
    implementation(libs.androidx.navigation.compose)

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
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
