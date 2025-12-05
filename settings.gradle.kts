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
includeBuild("mpp-idea")

include("core")

include(
    "pycharm",
    "java",
    "kotlin",
    "javascript",
    "goland",
    "rust",
)

include(
    "exts:ext-terminal",
    "exts:ext-git",
    "exts:ext-database",

    "exts:devins-lang"
)
