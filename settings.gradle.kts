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
    "exts:database",
    // since JetBrains also call `android.jar`, so we rename it to `ext-android`
    "exts:ext-android",
    "exts:ext-harmonyos",
)
