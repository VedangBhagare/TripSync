plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.tripsync_phone_app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.tripsync"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    // Java only
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Android UI
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0") // Material 3 widgets (XML)
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation(libs.activity)

    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    // Room (Java)
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // JWT
    implementation("com.auth0:java-jwt:4.3.0")

    // Password hashing
    implementation("org.mindrot:jbcrypt:0.4")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    // Lifecycle (Java – no KTX)
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.8.0")

    // Google Maps + Places
    implementation("com.google.android.gms:play-services-maps:18.1.0")
    implementation("com.google.android.libraries.places:places:3.2.0")
    implementation ("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.android.libraries.places:places:3.5.0")




    implementation ("com.google.android.gms:play-services-wearable:18.2.0")

    implementation ("androidx.recyclerview:recyclerview:1.3.2")

    implementation ("com.google.code.gson:gson:2.11.0")

    implementation("androidx.work:work-runtime:2.9.0")
}
