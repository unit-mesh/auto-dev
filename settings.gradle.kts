rootProject.name = "AutoDev-Intellij"

enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include("plugin")

include(
    "pycharm",
    "java",
    "kotlin",
    "javascript",
    "goland",
    "rust",
    "csharp",
    "cpp",
)
