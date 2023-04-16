package cc.unitmesh.devti.runconfig.config

import cc.unitmesh.devti.language.StoryConfig
import cc.unitmesh.devti.runconfig.command.BaseConfig

class DevtiCreateStoryConfigure(
    val storyId: Int,
    val storySource: String,
    val acs: List<String> = listOf()
) : BaseConfig() {
    override val configurationName = "DevTi Create Story"

    companion object {
        fun fromStoryConfig(storyConfig: StoryConfig): DevtiCreateStoryConfigure {
            return DevtiCreateStoryConfigure(storyConfig.storyId, storyConfig.storySource, storyConfig.acs)
        }
    }

    override fun toString(): String {
        return "DevtiCreateStoryConfigure(storyId=$storyId, storySource='$storySource', acs=$acs)"
    }
}