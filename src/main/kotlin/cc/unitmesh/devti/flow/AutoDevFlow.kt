package cc.unitmesh.devti.flow

import cc.unitmesh.devti.analysis.DtClass
import cc.unitmesh.devti.flow.kanban.Kanban
import cc.unitmesh.devti.flow.model.SimpleStory
import cc.unitmesh.devti.connector.DevtiFlowAction
import cc.unitmesh.devti.connector.openai.OpenAIConnector
import cc.unitmesh.devti.connector.openai.PromptGenerator
import cc.unitmesh.devti.flow.model.SimpleProjectInfo
import cc.unitmesh.devti.flow.model.TargetEndpoint
import cc.unitmesh.devti.gui.chat.ChatCodingComponent
import cc.unitmesh.devti.parser.parseCodeFromString
import cc.unitmesh.devti.runconfig.AutoDevRunProfileState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import kotlinx.coroutines.runBlocking

class AutoDevFlow(
    private val kanban: Kanban,
    private val connector: OpenAIConnector,
    private val processor: CrudProcessor? = null,
    val ui: ChatCodingComponent,
) : DevtiFlowAction {
    private val promptGenerator = PromptGenerator()

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

            storyDetail = fillStoryDetail(simpleProject, story.description)

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
        val targetEndpoint = analysisEndpoint(storyDetail, files)

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
            doExecuteUpdateEndpoint(target, storyDetail)
        } catch (e: Exception) {
            logger.warn("update method failed: $e, try to fill update method 2nd")
            doExecuteUpdateEndpoint(target, storyDetail)
        }
    }

    private fun doExecuteUpdateEndpoint(target: TargetEndpoint, storyDetail: String) {
        val codes = fetchForEndpoint(target.endpoint, target.controller, storyDetail)
        if (codes.isEmpty()) {
            logger.warn("update method code is empty, skip")
        } else {
            if (processor == null) return

            codes.indices.forEach { i ->
                val code = codes[i]
                createCodeByType(code, processor, target)
            }
        }
    }

    private fun createCodeByType(code: String, processor: CrudProcessor, target: TargetEndpoint) {
        when {
            processor.isController(code) -> {
                processor.createControllerOrUpdateMethod(target.controller.name, code, target.hasMatchedController)
            }

            processor.isService(code) -> {
                processor.createService(code)
            }

            processor.isEntity(code) -> {
                processor.createEntity(code)
            }

            processor.isDto(code) -> {
                processor.createDto(code)
            }

            else -> {
                processor.createClass(code, null)
            }
        }
    }

    private fun fetchForEndpoint(
        targetEndpoint: String,
        targetController: DtClass,
        storyDetail: String,
    ): List<String> {
        val content = needUpdateMethodOfController(targetEndpoint, targetController, storyDetail)
        val code = parseCodeFromString(content)
        logger.warn("update method code: $code")
        return code
    }

    override fun fillStoryDetail(project: SimpleProjectInfo, story: String): String {
        val promptText = promptGenerator.storyDetail(project, story)
        return executePrompt(promptText)
    }

    override fun analysisEndpoint(storyDetail: String, classes: List<DtClass>): String {
        val promptText = promptGenerator.createEndpoint(storyDetail, classes)
        return executePrompt(promptText)
    }

    override fun needUpdateMethodOfController(targetEndpoint: String, clazz: DtClass, storyDetail: String): String {
        val promptText = promptGenerator.updateControllerMethod(clazz, storyDetail)
        logger.warn("needUpdateMethodForController prompt text: $promptText")
        return executePrompt(promptText)
    }

    private fun executePrompt(promptText: String): String {
        ui.add(promptText, true)
        return runBlocking {
            val prompt = connector.prompt(promptText)
            ui.add(prompt, false)
            return@runBlocking prompt
        }
    }

    companion object {
        private val logger: Logger = logger<AutoDevRunProfileState>()
        private val regex = Regex("""(\w+Controller)""")

        fun matchControllerName(targetEndpoint: String): String? {
            val matchResult = regex.find(targetEndpoint)
            return matchResult?.groupValues?.get(1)
        }
    }
}

