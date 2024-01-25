rootProject.name = "AutoDev-Intellij"

enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include("plugin")

include(
    "python",
    "java",
    "kotlin",
    "javascript",
    "go",
    "rust",
    "csharp",
    "cpp",
    "scala",
)

include(
    "exts:database",
    "exts:vue",
    "exts:android",
//    todo split vcs
//    "exts:vcs",
)
