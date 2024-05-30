rootProject.name = "AutoDev-Intellij"

enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include("plugin")

include(
    "pycharm",
    "java",
    "kotlin",
    "javascript",
    // since JetBrains also call `go.jar`, so we rename it to `goland` for avoiding conflict
    "goland",
    "rust",
    "csharp",
    "cpp",
    "scala",
)

include(
    "local-bundle",

    "exts:ext-database",
    // since JetBrains also call `android.jar`, so we rename it to `ext-android`
    "exts:ext-android",
    "exts:ext-harmonyos",
    "exts:ext-terminal",
    // git4idea is the git plugin for IntelliJ IDEA, so we rename it to `exts-git`
    "exts:ext-git",
    // for http test
    "exts:ext-http-client",

    // the Input Language support for AutoDev
    "exts:devins-lang"
)
