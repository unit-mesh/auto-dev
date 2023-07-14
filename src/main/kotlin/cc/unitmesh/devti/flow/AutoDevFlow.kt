package cc.unitmesh.devti.flow

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.analysis.DtClass
import cc.unitmesh.devti.flow.kanban.Kanban
import cc.unitmesh.devti.flow.model.SimpleStory
import cc.unitmesh.devti.flow.base.DevtiFlowAction
import cc.unitmesh.devti.connector.openai.OpenAIConnector
import cc.unitmesh.devti.connector.openai.PromptTemplate
import cc.unitmesh.devti.flow.base.SpringBaseCrud
import cc.unitmesh.devti.flow.code.JavaParseUtil
import cc.unitmesh.devti.flow.model.SimpleProjectInfo
import cc.unitmesh.devti.flow.model.TargetEndpoint
import cc.unitmesh.devti.gui.chat.ChatCodingComponent
import cc.unitmesh.devti.parser.parseCodeFromString
import cc.unitmesh.devti.runconfig.AutoDevRunProfileState
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiFile
import kotlinx.coroutines.runBlocking

class AutoDevFlow(
    private val kanban: Kanban,
    private val connector: OpenAIConnector,
    private val processor: SpringBaseCrud? = null,
    val ui: ChatCodingComponent,
) : DevtiFlowAction {
    private val promptTemplate = PromptTemplate()
    private var selectedControllerName = ""
    private var selectedControllerCode = ""

    /**
     * Step 1: check story detail is valid, if not, fill story detail
     */
    override fun getOrCreateStoryDetail(id: String): String {
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
     * Step 2: base on story detail, generate dto and entity
     */
    override fun updateOrCreateDtoAndEntity(storyDetail: String) {
        val files: List<DtClass> = processor?.modelList() ?: emptyList()
        val promptText = promptTemplate.createDtoAndEntity(storyDetail, files)

        logger.warn("needUpdateMethodForController prompt text: $promptText")
        val result = executePrompt(promptText)

        parseCodeFromString(result).forEach { dto ->
            processor?.let { createCodeByType(dto) }
        }
    }

    /**
     * Step 3: fetch suggest endpoint, if not found, return null
     */
    override fun fetchSuggestEndpoint(storyDetail: String): TargetEndpoint {
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
     * Step 4: update endpoint method
     */
    override fun updateOrCreateEndpointCode(target: TargetEndpoint, storyDetail: String) {
        selectedControllerName = target.controller.name
        try {
            doExecuteUpdateEndpoint(target, storyDetail)
        } catch (e: Exception) {
            logger.warn("update method failed: $e, try to fill update method 2nd")
            doExecuteUpdateEndpoint(target, storyDetail)
        }
    }

    /**
     * Step 5: create service and repository
     */
    override fun updateOrCreateServiceAndRepository() {
        // filter controllerName == selectedControllerName
        val files: List<PsiFile> = processor?.getAllControllerFiles()?.filter { it.name == selectedControllerName }
            ?: emptyList()
        val controllerCode = if (files.isEmpty()) {
            selectedControllerCode
        } else {
            runReadAction {
                files[0].text
            }
        }

        val promptText = promptTemplate.createServiceAndRepository(controllerCode)

        logger.warn("createServiceAndController prompt text: $promptText")
        val result = executePrompt(promptText)

        val services = parseCodeFromString(result)
        services.forEach { service ->
            processor?.let { createCodeByType(service, true) }
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
                createCodeByType(code, target.isNeedToCreated, target.controller.name)
            }
        }
    }

    private fun createCodeByType(
        code: String,
        isNeedCreateController: Boolean = false,
        controllerName: String = "",
    ) {
        JavaParseUtil.splitClass(code).forEach {
            createCode(it, controllerName, isNeedCreateController)
        }
    }

    private fun createCode(
        code: String,
        controllerName: String,
        isNeedCreateController: Boolean
    ) {
        when {
            processor!!.isController(code) -> {
                selectedControllerCode = code
                processor.createControllerOrUpdateMethod(controllerName, code, isNeedCreateController)
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

            processor.isRepository(code) -> {
                processor.createRepository(code)
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

    fun fillStoryDetail(project: SimpleProjectInfo, story: String): String {
        val promptText = promptTemplate.storyDetail(project, story)
        return executePrompt(promptText)
    }

    fun analysisEndpoint(storyDetail: String, classes: List<DtClass>): String {
        val promptText = promptTemplate.createEndpoint(storyDetail, classes)
        return executePrompt(promptText)
    }

    fun needUpdateMethodOfController(targetEndpoint: String, clazz: DtClass, storyDetail: String): String {
        val allModels = processor?.modelList()?.map { it } ?: emptyList()
        val relevantName = targetEndpoint.replace("Controller", "")

        // filter *Request, *Response
        val dtos = allModels.filter {
            it.name.contains(relevantName) && (it.name.endsWith("Request") || it.name.endsWith("Response"))
        }

        // relevant entity = xxController -> xx
        val relevantDto = allModels.find { it.name.startsWith(relevantName) }

        val models = if (relevantDto != null) {
            dtos + relevantDto
        } else {
            dtos
        }

        val services = processor?.serviceList()?.map { it } ?: emptyList()

        val promptText = promptTemplate.updateControllerMethod(clazz, storyDetail, models, services)
        logger.warn("needUpdateMethodForController prompt text: $promptText")
        return executePrompt(promptText)
    }

    private fun executePrompt(promptText: String): String {
        ui.add(promptText, true)

        // for answer
        ui.add(AutoDevBundle.message("devti.loading"))

        return runBlocking {
            val prompt = connector.stream(promptText)
            val result = ui.updateMessage(prompt)
            return@runBlocking result
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

