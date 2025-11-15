rootProject.name = "AutoDev-Intellij"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

include("mpp-linter")
include("mpp-core")
include("mpp-ui")
include("mpp-codegraph")
include("mpp-server")

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
    "exts:ext-database",
    "exts:ext-terminal",
    // git4idea is the git plugin for IntelliJ IDEA, so we rename it to `exts-git`
    "exts:ext-git",
    // for http test
    "exts:ext-http-client",
    "exts:ext-plantuml",
    "exts:ext-mermaid",
    "exts:ext-endpoints",
    "exts:ext-vue",
    "exts:ext-dependencies",
    "exts:ext-container",
    "exts:ext-openrewrite",
    "exts:ext-wechat",
    "exts:ext-diagram",

    // the Input Language support for AutoDev
    "exts:devins-lang"
)
