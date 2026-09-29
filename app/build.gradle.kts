import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

android {
    namespace = "com.example.hubretro"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.hubretro"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "EBAY_CLIENT_ID",       "\"${localProps["EBAY_CLIENT_ID"] ?: ""}\"")
        buildConfigField("String", "EBAY_CLIENT_SECRET",   "\"${localProps["EBAY_CLIENT_SECRET"] ?: ""}\"")
        buildConfigField("String", "UNSPLASH_ACCESS_KEY",  "\"${localProps["UNSPLASH_ACCESS_KEY"] ?: ""}\"")
        buildConfigField("String", "IGDB_CLIENT_ID",       "\"${localProps["IGDB_CLIENT_ID"] ?: ""}\"")
        buildConfigField("String", "IGDB_CLIENT_SECRET",   "\"${localProps["IGDB_CLIENT_SECRET"] ?: ""}\"")
        buildConfigField("String", "YOUTUBE_API_KEY",      "\"${localProps["YOUTUBE_API_KEY"] ?: ""}\"")
        buildConfigField("String", "TWITCH_CLIENT_ID",     "\"${localProps["TWITCH_CLIENT_ID"] ?: ""}\"")
        buildConfigField("String", "TWITCH_CLIENT_SECRET", "\"${localProps["TWITCH_CLIENT_SECRET"] ?: ""}\"")
        buildConfigField("String", "CLAUDE_API_KEY",       "\"${localProps["CLAUDE_API_KEY"] ?: ""}\"")
        buildConfigField("String", "NEWS_API_KEY",         "\"${localProps["NEWS_API_KEY"] ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:4.12.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.compose.foundation:foundation")


    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.1.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.firebase:firebase-firestore")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-messaging")

    // Networking

    implementation("org.jsoup:jsoup:1.17.2") {
        exclude(group = "org.apache.commons", module = "commons-math3")
    }

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2")

    // Coil
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-gif:2.6.0")

    // YouTube Player — exclude commons-math3 to fix ComplexDouble operator conflict
    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:12.1.0") {
        exclude(group = "org.apache.commons", module = "commons-math3")
    }

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}