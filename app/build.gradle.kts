plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.linkgrab"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.linkgrab"
        minSdk = 29
        targetSdk = 34
        versionCode = 2
        versionName = "1.1.0"
        ndk {
            abiFilters += setOf(providers.gradleProperty("testAbi").getOrElse("arm64-v8a"))
        }
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}
dependencies {
    val version = "0.18.1"
    implementation("io.github.junkfood02.youtubedl-android:library:$version")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:$version")
}

