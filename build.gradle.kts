plugins {
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.ktor) apply false
    id("com.android.library") version "8.10.0" apply false
    id("com.android.application") version "8.10.0" apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
