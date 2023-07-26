rootProject.name = "intellij-autodev"

enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include("plugin")

include(
    "pycharm",
    "idea",
    "kotlin",
    "webstorm",
)
