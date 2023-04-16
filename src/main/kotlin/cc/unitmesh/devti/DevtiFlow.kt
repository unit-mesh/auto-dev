package cc.unitmesh.devti

import cc.unitmesh.devti.analysis.CrudProcessor
import cc.unitmesh.devti.analysis.DtClass
import cc.unitmesh.devti.kanban.Kanban
import cc.unitmesh.devti.kanban.SimpleStory
import cc.unitmesh.devti.prompt.DevtiFlowAction
import cc.unitmesh.devti.runconfig.DtRunState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger

class DevtiFlow(
    private val kanban: Kanban,
    private val flowAction: DevtiFlowAction,
    private val analyser: CrudProcessor? = null
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

        val files: List<DtClass> = analyser?.controllerList() ?: emptyList()
        logger.info("start devti flow")
        flowAction.analysisEndpoint(storyDetail, files)
    }

    companion object {
        private val logger: Logger = logger<DtRunState>()
    }
}
