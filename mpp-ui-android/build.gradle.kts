plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "cc.unitmesh.devins.ui.android"
    compileSdk = 35
    
    defaultConfig {
        applicationId = "cc.unitmesh.devins.ui.android"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false
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
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    implementation(project(":mpp-ui"))
}

