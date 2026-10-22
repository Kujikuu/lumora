import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// The app's theme, fonts and focus-aware building blocks, shared by the app and the launcher APK.
android {
    namespace = "com.iptvcinema.tv.core.design"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // api: consumers compose with these types directly and must resolve the same versions.
    api(platform(libs.androidx.compose.bom))
    // The app's other dependencies already resolve Compose to this version; pinning it here keeps
    // every module that shares this library (app, launcher) compiling against the same API.
    api("androidx.compose.runtime:runtime:${libs.versions.compose.resolved.get()}")
    api("androidx.compose.ui:ui:${libs.versions.compose.resolved.get()}")
    api("androidx.compose.foundation:foundation:${libs.versions.compose.resolved.get()}")
    api(libs.androidx.tv.material)
    api(libs.coil.compose)

    testImplementation(libs.junit)
}
