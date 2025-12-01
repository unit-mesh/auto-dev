rootProject.name = "mpp-idea"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        kotlin("jvm") version "2.2.0"
        kotlin("plugin.compose") version "2.2.0"
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
// - devins-lang, core: uses root project name "AutoDev-Intellij" as group
includeBuild("..") {
    dependencySubstitution {
        // Substitute Maven coordinates with project dependencies
        substitute(module("AutoDev-Intellij:mpp-ui")).using(project(":mpp-ui")).because("Using local project")
        substitute(module("cc.unitmesh:mpp-core")).using(project(":mpp-core")).because("Using local project")
        substitute(module("AutoDev-Intellij:mpp-codegraph")).using(project(":mpp-codegraph")).because("Using local project")
        substitute(module("cc.unitmesh.viewer:mpp-viewer")).using(project(":mpp-viewer")).because("Using local project")
        // DevIn language support for @ and / completion
        substitute(module("AutoDev-Intellij:exts-devins-lang")).using(project(":exts:devins-lang")).because("Using local project")
        substitute(module("AutoDev-Intellij:core")).using(project(":core")).because("Using local project")
    }
}

