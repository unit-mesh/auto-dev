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
// For KMP projects, we substitute the Maven coordinates with local project dependencies
// Note: The group IDs must match what's defined in the respective build.gradle.kts files:
// - mpp-ui: uses root project name "AutoDev-Intellij" as group
// - mpp-core: group = "cc.unitmesh"
// - mpp-codegraph: uses root project name
// - mpp-viewer: group = "cc.unitmesh.viewer"
includeBuild("..") {
    dependencySubstitution {
        // Substitute Maven coordinates with project dependencies
        substitute(module("AutoDev-Intellij:mpp-ui")).using(project(":mpp-ui")).because("Using local project")
        substitute(module("cc.unitmesh:mpp-core")).using(project(":mpp-core")).because("Using local project")
        substitute(module("AutoDev-Intellij:mpp-codegraph")).using(project(":mpp-codegraph")).because("Using local project")
        substitute(module("cc.unitmesh.viewer:mpp-viewer")).using(project(":mpp-viewer")).because("Using local project")
    }
}

