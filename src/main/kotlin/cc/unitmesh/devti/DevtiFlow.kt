package cc.unitmesh.devti

import cc.unitmesh.devti.kanban.Kanban
import cc.unitmesh.devti.kanban.SimpleStory
import cc.unitmesh.devti.prompt.DevtiFlowAction

class DevtiFlow(
    private val kanban: Kanban,
    private val devtiFlowAction: DevtiFlowAction
) {
    fun start(id: String) {
        val project = kanban.getProjectInfo()
        val story = kanban.getStoryById(id)

        var storyDetail = story.description
        if (!kanban.isValidStory(storyDetail)) {
            storyDetail = devtiFlowAction.fillStoryDetail(project, story.description)
            val newStory = SimpleStory(story.id, story.title, storyDetail)
            kanban.updateStoryDetail(newStory)
        }
    }
}
