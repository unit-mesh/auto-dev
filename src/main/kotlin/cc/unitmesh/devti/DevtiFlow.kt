package cc.unitmesh.devti

import cc.unitmesh.devti.kanban.Kanban
import cc.unitmesh.devti.kanban.SimpleStory
import cc.unitmesh.devti.prompt.DevtiFlowAction
import cc.unitmesh.devti.runconfig.DtRunState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger

class DevtiFlow(
    private val kanban: Kanban,
    private val flowAction: DevtiFlowAction
) {
    fun start(id: String) {
        val project = kanban.getProjectInfo()
        val story = kanban.getStoryById(id)

        var storyDetail = story.description
        if (!kanban.isValidStory(storyDetail)) {
            logger.info("story detail is not valid, fill story detail")

            storyDetail = flowAction.fillStoryDetail(project, story.description)

            logger.info("fill story detail: $storyDetail")
            val newStory = SimpleStory(story.id, story.title, storyDetail)
            kanban.updateStoryDetail(newStory)
        }

        logger.info("start devti flow")
        flowAction.analysisEndpoint(project, storyDetail)
    }

    companion object {
        private val logger: Logger = logger<DtRunState>()
    }
}
