package cc.unitmesh.devti.kanban

interface Kanban {
    fun isValidStory(content: String): Boolean {
        return content.startsWith("###DevTi")
    }

    fun getProjectInfo(): SimpleProjectInfo
    fun getStories(): List<SimpleStory>
    fun getStoryById(storyId: String): SimpleStory

    /**
     * update story details
     */
    fun updateStoryDetail(simpleStory: SimpleStory)
}
