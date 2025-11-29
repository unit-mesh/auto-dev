rootProject.name = "mpp-idea"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        kotlin("jvm") version "2.1.20"
        kotlin("plugin.compose") version "2.1.20"
        kotlin("plugin.serialization") version "2.1.20"
        id("org.jetbrains.intellij.platform") version "2.10.2"
    }
}

// Include mpp-ui from parent project for shared UI components and ConfigManager
// For KMP projects, we substitute the JVM target artifacts
includeBuild("..") {
    dependencySubstitution {
        substitute(module("cc.unitmesh.devins:mpp-ui-jvm")).using(project(":mpp-ui"))
        substitute(module("cc.unitmesh.devins:mpp-core-jvm")).using(project(":mpp-core"))
    }
}

