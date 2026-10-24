plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
}

val releaseKeystorePath = System.getenv("ZCUT_KEYSTORE_PATH")
val releaseStorePassword = System.getenv("ZCUT_KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("ZCUT_KEY_ALIAS")
val releaseKeyPassword = System.getenv("ZCUT_KEY_PASSWORD")
val releaseSigningReady = listOf(
    releaseKeystorePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.zaaam.zcut"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zaaam.zcut"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = System.getenv("ZCUT_VERSION_NAME")?.removePrefix("v") ?: "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        if (releaseSigningReady) {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (releaseSigningReady) {
                signingConfig = signingConfigs.getByName("release")
            }
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
        buildConfig = true
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Compose UI stack
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.core:core-splashscreen:1.0.1")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Render stack (media3) is added in M3, when the preview player is wired.
    // Shipping unused ExoPlayer/Transformer modules now only grows the APK.

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
