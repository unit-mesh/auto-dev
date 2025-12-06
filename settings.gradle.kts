rootProject.name = "Xiiu"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

//include("mpp-linter")
include("mpp-core")
include("mpp-ui")
include("mpp-codegraph")
include("mpp-server")
include("mpp-viewer")
include("mpp-viewer-web")
include("xuiper-ui")

// IDEA plugin as composite build
includeBuild("mpp-idea")
