plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.quickscan.qr"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.quickscan.qr"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
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
}

dependencies {

    // Android
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.ui:ui-viewbinding")

    // CameraX
    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")

    // Google ML Kit Barcode Scanner
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // ZXing QR generator
    implementation("com.google.zxing:core:3.5.3")

    // Google AdMob
    implementation("com.google.android.gms:play-services-ads:24.7.0")

    // AppCompat / Material
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}
