rootProject.name = "intellij-autodev"

enableFeaturePreview("VERSION_CATALOGS")

include("plugin")

include(
    "pycharm",
    "idea",
    "webstorm",
)
