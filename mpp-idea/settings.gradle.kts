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

// Include all IDEA plugin modules
include("mpp-idea-core")

// Language support modules (parent dir doesn't need to be included separately)
include(
    "mpp-idea-lang:pycharm",
    "mpp-idea-lang:java",
    "mpp-idea-lang:kotlin",
    "mpp-idea-lang:javascript",
    "mpp-idea-lang:goland",
    "mpp-idea-lang:rust",
)

// Extension modules (parent dir doesn't need to be included separately)
include(
    "mpp-idea-exts:ext-terminal",
    "mpp-idea-exts:ext-git",
    "mpp-idea-exts:ext-database",
    "mpp-idea-exts:devins-lang"
)

// Include mpp-ui from parent project for shared UI components and ConfigManager
// For KMP projects, we substitute the Maven coordinates with local project dependencies
// Note: The group IDs must match what's defined in the respective build.gradle.kts files:
// - mpp-ui: uses root project name "Xiiu" as group
// - mpp-core: group = "cc.unitmesh"
// - mpp-codegraph: uses root project name
// - mpp-viewer: group = "cc.unitmesh.viewer"
includeBuild("..") {
    dependencySubstitution {
        // Substitute Maven coordinates with project dependencies
        substitute(module("Xiiu:mpp-ui")).using(project(":mpp-ui")).because("Using local project")
        substitute(module("cc.unitmesh:mpp-core")).using(project(":mpp-core")).because("Using local project")
        substitute(module("Xiiu:mpp-codegraph")).using(project(":mpp-codegraph")).because("Using local project")
        substitute(module("cc.unitmesh.viewer:mpp-viewer")).using(project(":mpp-viewer")).because("Using local project")
    }
}
