plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.tripsync_wear_app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.tripsync"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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

    buildFeatures {
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.play.services.wearable)

    implementation ("androidx.core:core-ktx:1.13.1")
    implementation ("androidx.appcompat:appcompat:1.7.0")
    implementation ("com.google.android.material:material:1.12.0")
    implementation ("androidx.recyclerview:recyclerview:1.3.2")
    implementation ("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    implementation ("com.google.android.gms:play-services-wearable:18.2.0")
    implementation ("androidx.wear:wear:1.3.0")
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation ("androidx.lifecycle:lifecycle-runtime:2.8.3")

    // Notifications
    implementation ("androidx.core:core:1.13.1")
    implementation(libs.activity)

    implementation ("androidx.wear:wear:1.3.0")


}