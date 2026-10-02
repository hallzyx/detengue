plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.hallzyx.detengue"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hallzyx.detengue"
        // Android 8.0+: cubre prácticamente todo el parque de gama baja vigente
        // y permite iconos adaptativos sin duplicar recursos legacy.
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        // PRD §45: el modo demo nunca falsifica detecciones; solo activa métricas/overlay.
        buildConfigField("boolean", "DEMO_MODE", "false")

        // Solo ABIs reales de teléfono (+ x86_64 para el emulador): recorta el tamaño del APK.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    // El modelo vive en android/models/ (PRD §58) y se empaqueta como asset.
    sourceSets {
        getByName("main") {
            assets.srcDirs("../models")
        }
    }

    // El .tflite se mapea en memoria directamente desde el APK: no debe comprimirse.
    androidResources {
        noCompress += "tflite"
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "DEMO_MODE", "true")
        }
        release {
            isMinifyEnabled = false
            // APK instalable para la demo del hackathon. Sustituir por una keystore propia para publicar.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    implementation(libs.kotlinx.coroutines.android)

    // Inferencia local (CPU + XNNPACK). Sin servicios de Google Play: funciona 100 % offline.
    implementation(libs.tensorflow.lite)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
