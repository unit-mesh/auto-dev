package cc.unitmesh.devti

import cc.unitmesh.devti.analysis.CrudProcessor
import cc.unitmesh.devti.analysis.DtClass
import cc.unitmesh.devti.kanban.Kanban
import cc.unitmesh.devti.kanban.SimpleStory
import cc.unitmesh.devti.connector.DevtiFlowAction
import cc.unitmesh.devti.prompt.parseCodeFromString
import cc.unitmesh.devti.runconfig.AutoCRUDState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger

data class TargetEndpoint(
    val endpoint: String,
    var controller: DtClass,
    val hasMatchedController: Boolean = true
)

class DevtiFlow(
    private val kanban: Kanban,
    private val flowAction: DevtiFlowAction,
    private val processor: CrudProcessor? = null
) {
    /**
     * Step 1: check story detail is valid, if not, fill story detail
     */
    fun fillStoryDetail(id: String): String {
        val simpleProject = kanban.getProjectInfo()
        val story = kanban.getStoryById(id)

        // 1. check story detail is valid, if not, fill story detail
        var storyDetail = story.description
        if (!kanban.isValidStory(storyDetail)) {
            logger.warn("story detail is not valid, fill story detail")

            storyDetail = flowAction.fillStoryDetail(simpleProject, story.description)

            val newStory = SimpleStory(story.id, story.title, storyDetail)
            kanban.updateStoryDetail(newStory)
        }
        logger.warn("user story detail: $storyDetail")
        return storyDetail
    }

    /**
     * Step 2: fetch suggest endpoint, if not found, return null
     */
    fun fetchSuggestEndpoint(storyDetail: String): TargetEndpoint {
        val files: List<DtClass> = processor?.controllerList() ?: emptyList()
        logger.warn("start devti flow")
        val targetEndpoint = flowAction.analysisEndpoint(storyDetail, files)
        // use regex match *Controller from targetEndpoint
        val controller = matchControllerName(targetEndpoint)
        if (controller == null) {
            logger.warn("no controller found from: $controller")
            return TargetEndpoint("", DtClass("", listOf()), false)
        }

        logger.warn("target endpoint: $controller")
        val targetController = files.find { it.name == controller }
        if (targetController == null) {
            logger.warn("no controller found from: $controller")
            return TargetEndpoint(controller, DtClass(controller, listOf()), false)
        }

        return TargetEndpoint(controller, targetController)
    }

    /**
     * Step 3: update endpoint method
     */
    fun updateEndpointMethod(target: TargetEndpoint, storyDetail: String) {
        try {
            val code = fetchEndpoint(target.endpoint, target.controller, storyDetail).trimIndent()
            if (code.isEmpty()) {
                logger.warn("update method code is empty, skip")
            } else {
                processor?.createControllerOrUpdateMethod(target.controller.name, code, target.hasMatchedController)
            }
        } catch (e: Exception) {
            logger.warn("update method failed: $e, try to fill update method 2nd")

            val code = fetchEndpoint(target.endpoint, target.controller, storyDetail).trimIndent()
            if (code.isEmpty()) {
                logger.warn("update method code is empty, skip")
            } else {
                processor?.createControllerOrUpdateMethod(target.controller.name, code, target.hasMatchedController)
            }
        }
    }

    private fun fetchEndpoint(
        targetEndpoint: String,
        targetController: DtClass,
        storyDetail: String
    ): String {
        val content = flowAction.needUpdateMethodOfController(targetEndpoint, targetController, storyDetail)
        val code = parseCodeFromString(content)
        logger.warn("update method code: $code")
        return code
    }

    companion object {
        private val logger: Logger = logger<AutoCRUDState>()
        private val regex = Regex("""(\w+Controller)""")

        fun matchControllerName(targetEndpoint: String): String? {
            val matchResult = regex.find(targetEndpoint)
            return matchResult?.groupValues?.get(1)
        }
    }
}

