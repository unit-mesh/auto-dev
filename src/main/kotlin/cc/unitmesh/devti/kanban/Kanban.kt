package cc.unitmesh.devti.kanban

interface Kanban {
    fun getProjectInfo(): SimpleProjectInfo
    fun getStories(): List<SimpleStory>
    fun getStoryById(storyId: String): SimpleStory

    /**
     * update story details
     */
    fun updateStoryDetail(simpleStory: SimpleStory)
}

/**
 * A simple story
 */
interface SimpleStory {
    val id: String
    val title: String
    val description: String
    val status: String
}

/**
 * for GPT to generate stories, like:
 * 网站信息
 *
 * """
 * phodal.com (A Growth Engineering Blog)
 * """
 */
class SimpleProjectInfo(
    val id: String,
    val name: String,
    val description: String
) {
}