import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "top.blackcyan.collins"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    // Release signing is configured entirely from environment variables so no
    // secret ever lands in the repo or Gradle output:
    //   ANDROID_KEYSTORE_PATH  – path to the .jks/.p12 file
    //   ANDROID_KEYSTORE_PASSWORD
    //   ANDROID_KEY_ALIAS
    //   ANDROID_KEY_PASSWORD
    // When they are absent (local builds without CI secrets) release APKs stay
    // unsigned; the release workflow fails early instead of publishing one.
    val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
    val hasReleaseSigning = !keystorePath.isNullOrBlank() &&
        System.getenv("ANDROID_KEY_ALIAS") != null

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    defaultConfig {
        applicationId = "top.blackcyan.collins"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = providers.gradleProperty("appVersionCode").get().toInt()
        versionName = providers.gradleProperty("appVersion").get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}