import java.net.URI
import java.security.MessageDigest

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
}
val bundledYtDlpVersion = "2026.08.19"
val bundledYtDlpSha256 = "1fa6733c37ea6fb51c99ad8fe785e7b7e5f3246c9b980230329d4fb72ed8d4d6"
val bundledEngineResources = layout.buildDirectory.dir("generated/bundled-engine/res")
val prepareBundledEngine = tasks.register("prepareBundledEngine") {
    inputs.property("version", bundledYtDlpVersion)
    inputs.property("sha256", bundledYtDlpSha256)
    val output = bundledEngineResources.map { it.file("raw/ytdlp") }
    outputs.file(output)
    doLast {
        val connection = URI("https://github.com/yt-dlp/yt-dlp/releases/download/$bundledYtDlpVersion/yt-dlp")
            .toURL().openConnection().apply { connectTimeout = 20_000; readTimeout = 60_000 }
        val bytes = connection.getInputStream().use { it.readBytes() }
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        check(digest == bundledYtDlpSha256) { "Bundled yt-dlp SHA-256 mismatch" }
        output.get().asFile.apply { parentFile.mkdirs(); writeBytes(bytes) }
    }
}
android {
    namespace = "com.example.linkgrab"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.linkgrab"
        minSdk = 29
        targetSdk = 36
        versionCode = 21
        versionName = "2.0.1"
        buildConfigField("String", "BUNDLED_YTDLP_VERSION", "\"$bundledYtDlpVersion\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk { abiFilters += setOf(providers.gradleProperty("testAbi").getOrElse("arm64-v8a")) }
    }
    buildFeatures { compose = true; buildConfig = true }
    sourceSets["main"].res.srcDir(bundledEngineResources)
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging {
        jniLibs.useLegacyPackaging = true
        resources.excludes += setOf("META-INF/AL2.0", "META-INF/LGPL2.1")
    }
    signingConfigs {
        create("publicRelease") {
            val path = providers.environmentVariable("LINKGRAB_KEYSTORE").orNull
            if (!path.isNullOrBlank()) {
                storeFile = file(path)
                storePassword = providers.environmentVariable("LINKGRAB_STORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("LINKGRAB_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("LINKGRAB_KEY_PASSWORD").get()
            }
        }
    }
    buildTypes {
        release {
            if (providers.environmentVariable("LINKGRAB_KEYSTORE").isPresent) signingConfig = signingConfigs.getByName("publicRelease")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    testOptions { animationsDisabled = true }
}
tasks.named("preBuild").configure { dependsOn(prepareBundledEngine) }
kapt { arguments { arg("room.schemaLocation", "$projectDir/schemas") } }
dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.05.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    kapt("androidx.room:room-compiler:2.7.1")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.github.junkfood02.youtubedl-android:library:0.18.1")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
