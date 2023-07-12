package cc.unitmesh.devti.flow.model

import kotlinx.serialization.Serializable

@Serializable
class StoryConfig(
    val storyId: Int,
    val storySource: String,
    val acs: List<String> = listOf()
) {
    override fun toString(): String {
        return "StoryConfig(storyId=$storyId, storySource='$storySource', acs=$acs)"
    }
}