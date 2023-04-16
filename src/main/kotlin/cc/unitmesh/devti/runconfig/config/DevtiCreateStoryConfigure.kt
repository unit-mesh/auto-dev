package cc.unitmesh.devti.runconfig.config

import cc.unitmesh.devti.language.StoryConfig
import cc.unitmesh.devti.runconfig.command.BaseConfig

class DevtiCreateStoryConfigure(
    var storyConfig: StoryConfig? = null
) : BaseConfig() {
    override val configurationName = "DevTi Configure"

    companion object {
        fun getDefault(): DevtiCreateStoryConfigure {
            return DevtiCreateStoryConfigure(

            )
        }
    }

    override fun toString(): String {
        return "DevtiCreateStoryConfigure(storyConfig=$storyConfig)"
    }
}