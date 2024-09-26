package cc.unitmesh.devti.runconfig.config

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.flow.model.StoryConfig
import cc.unitmesh.devti.runconfig.command.BaseConfig

class AutoDevStory(
    val storyId: Int,
    private val storySource: String,
    private val acs: List<String> = listOf()
) : BaseConfig() {
    override var configurationName = AutoDevBundle.message("name") + "Create Story"

    init {
        if (storyId <= 0) {
            throw IllegalArgumentException("Story id must be greater than 0")
        }

        // update configure name by story id
        configurationName += " $storyId"
    }

    companion object {
        fun fromStoryConfig(storyConfig: StoryConfig): AutoDevStory {
            return AutoDevStory(storyConfig.storyId, storyConfig.storySource, storyConfig.acs)
        }
    }

    override fun toString(): String {
        return "DevtiCreateStoryConfigure(storyId=$storyId, storySource='$storySource', acs=$acs)"
    }
}