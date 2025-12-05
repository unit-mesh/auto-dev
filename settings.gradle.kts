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

include("mpp-idea-core")

include(
    "mpp-idea-lang:pycharm",
    "mpp-idea-lang:java",
    "mpp-idea-lang:kotlin",
    "mpp-idea-lang:javascript",
    "mpp-idea-lang:goland",
    "mpp-idea-lang:rust",
)

include(
    "mpp-idea-exts:ext-terminal",
    "mpp-idea-exts:ext-git",
    "mpp-idea-exts:ext-database",
    "mpp-idea-exts:devins-lang"
)
