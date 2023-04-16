package cc.unitmesh.devti.kanban

interface Kanban {
    fun isValidStory(content: String): Boolean {
        return content.contains("用户故事")
    }

    fun getProjectInfo(): SimpleProjectInfo
    fun getStories(): List<SimpleStory>
    fun getStoryById(storyId: String): SimpleStory
    fun updateStoryDetail(simpleStory: SimpleStory)
}
