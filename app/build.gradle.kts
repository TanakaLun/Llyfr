import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room3)
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "io.github.tanakalun.mynotes"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.tanakalun.mynotes"
        minSdk = 33
        targetSdk = 37
        versionCode = 45
        versionName = "1.3.2"
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
            excludes += "/META-INF/*.kotlin_module"
            excludes += "/kotlin/**"
            excludes += "DebugProbesKt.bin"
        }
        jniLibs {
            // Module-specific exclusions
            excludes += setOf(
                "lib/*/libandroidx.graphics.path.so",
                "lib/*/libdatastore_shared_counter.so"
            )
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(libs.core)
    implementation(libs.core.splashscreen)
    implementation(libs.miuix.ui)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.blur)
    implementation(libs.miuix.nav)
    implementation(libs.miuix.icons)
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.runtime)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    implementation(libs.room3.runtime)
    implementation(libs.sqlite.framework)
    ksp(libs.room3.compiler)

    implementation(libs.aboutlibraries.core)
    implementation(libs.richeditor)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}