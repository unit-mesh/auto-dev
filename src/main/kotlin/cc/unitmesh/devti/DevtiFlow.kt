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
        val targetEndpoint = flowAction.analysisEndpoint(storyDetail, files)
        // use regex match *Controller from targetEndpoint
        val controller = getController(targetEndpoint)
        if (controller == null) {
            logger.info("no controller found from: $targetEndpoint")
            return
        }

        logger.info("target endpoint: $targetEndpoint")
        val targetController = files.find { it.name == targetEndpoint }
        if (targetController == null) {
            logger.info("no controller found from: $targetEndpoint")
            return
        }

        val code = flowAction.needUpdateMethodForController(targetEndpoint, targetController)
        logger.info("update method code: $code")
    }

    private fun getController(targetEndpoint: String): String? {
        val regex = Regex("""\s+(\w+Controller)""")
        val matchResult = regex.find(targetEndpoint)
        return matchResult?.groupValues?.get(1)
    }

    companion object {
        private val logger: Logger = logger<DtRunState>()
    }
}
