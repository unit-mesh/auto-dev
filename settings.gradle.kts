rootProject.name = "AutoDev-Intellij"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include("core");

include(
    "pycharm",
    "java",
    "kotlin",
    "javascript",
    "goland",
    "rust",
)

include(
    "local-bundle",

    "exts:ext-database",
    "exts:ext-terminal",
    // git4idea is the git plugin for IntelliJ IDEA, so we rename it to `exts-git`
    "exts:ext-git",
    // for http test
    "exts:ext-http-client",
    "exts:ext-plantuml",
    "exts:ext-mermaid",

    // the Input Language support for AutoDev
    "exts:devins-lang"
)
