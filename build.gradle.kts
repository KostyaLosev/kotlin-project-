plugins {
    id("com.android.application") version "9.2.0"
}

android {
    namespace = "com.example.travelmemories"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.travelmemories"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
