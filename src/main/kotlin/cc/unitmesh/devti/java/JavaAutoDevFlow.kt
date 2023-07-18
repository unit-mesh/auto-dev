package cc.unitmesh.devti.java

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.context.DtClass
import cc.unitmesh.devti.models.openai.OpenAIProvider
import cc.unitmesh.devti.models.openai.PromptTemplate
import cc.unitmesh.devti.flow.base.CrudFlowProvider
import cc.unitmesh.devti.flow.code.JavaParseUtil
import cc.unitmesh.devti.flow.kanban.Kanban
import cc.unitmesh.devti.flow.model.SimpleStory
import cc.unitmesh.devti.flow.model.TargetEndpoint
import cc.unitmesh.devti.gui.chat.ChatCodingComponent
import cc.unitmesh.devti.parser.parseCodeFromString
import cc.unitmesh.devti.prompting.PromptStrategy
import cc.unitmesh.devti.runconfig.AutoDevRunProfileState
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import kotlinx.coroutines.runBlocking

class JavaAutoDevFlow(
    private val kanban: Kanban,
    private val connector: OpenAIProvider,
    private val processor: SpringBaseCrud? = null,
    val ui: ChatCodingComponent,
    val project: Project,
) : CrudFlowProvider {
    private val promptTemplate = PromptTemplate()
    private var selectedControllerName = ""
    private var selectedControllerCode = ""
    private var isNewController = false
    private val promptStrategy = PromptStrategy.strategy()!!

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

            storyDetail = run {
                val promptText = promptTemplate.storyDetail(simpleProject, story.description)
                executePrompt(promptText)
            }

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
        val promptText = promptTemplate.createEndpoint(storyDetail, files)
        val targetEndpoint = executePrompt(promptText)

        val controller = matchControllerName(targetEndpoint)
        if (controller == null) {
            logger.warn("no controller found from: $controller")
            return TargetEndpoint("", DtClass("", listOf()), false)
        }

        logger.warn("target endpoint: $controller")
        val targetController = files.find { it.name == controller }
        if (targetController == null) {
            isNewController = true
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
            doExecuteUpdateEndpoint(target, storyDetail, isNewController)
        } catch (e: Exception) {
            logger.warn("update method failed: $e, try to fill update method 2nd")
            doExecuteUpdateEndpoint(target, storyDetail, isNewController)
        }
    }

    /**
     * Step 5: create service and repository
     */
    override fun updateOrCreateServiceAndRepository() {
        val serviceName = selectedControllerName.removeSuffix("Controller") + "Service"
//        // check service is exist
        val files: List<PsiFile> = processor?.getAllServiceFiles()?.filter { it.name == "$serviceName.java" }
            ?: emptyList()

        if (files.isNotEmpty()) {
            updateServiceMethod(files.first() as PsiJavaFile, serviceName)
        } else {
            createServiceFile(serviceName)
        }
    }

    // TODO: update service method
    private fun updateServiceMethod(serviceFile: PsiJavaFile, serviceName: String) {
        // 1. filter used method from selectedControllerCode.
        val usedCode = JavaCodeProcessor.findUsageCode(selectedControllerCode, serviceName)

        // 2. if serviceFile exist used method, skip
        // TODO: or also send service code to server ???
        val noExistMethods = JavaCodeProcessor.findNoExistMethod(serviceFile, usedCode)
        if (noExistMethods.isEmpty()) {
            logger.warn("no need to update service method")
            return
        }

        // 3. if serviceFile not exist used method, send service code to openai
        val finalPrompt = promptStrategy.advice(serviceFile, usedCode, noExistMethods)
        val promptText = promptTemplate.updateServiceMethod(finalPrompt)
        val result = executePrompt(promptText)
        val services = parseCodeFromString(result)
        services.forEach { code ->
            processor?.updateMethod(serviceFile, serviceName, code)
        }
    }

    private fun createServiceFile(serviceName: String) {
        val controllerFile: List<PsiFile> =
            processor?.getAllControllerFiles()?.filter { it.name == selectedControllerName }
                ?: emptyList()

        val controllerCode = if (controllerFile.isEmpty()) {
            selectedControllerCode
        } else {
            runReadAction {
                promptStrategy.advice(controllerFile.first() as PsiJavaFile, serviceName).prefixCode
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


    private fun doExecuteUpdateEndpoint(target: TargetEndpoint, storyDetail: String, isNewController: Boolean) {
        val codes = fetchForEndpoint(target.endpoint, target.controller, storyDetail, isNewController)
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
            isNeedCreateController || processor!!.isController(code) -> {
                selectedControllerCode = code
                processor!!.createControllerOrUpdateMethod(controllerName, code, isNeedCreateController)
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
        isNewController: Boolean,
    ): List<String> {
        val content = needUpdateMethodOfController(targetEndpoint, targetController, storyDetail, isNewController)
        val code = parseCodeFromString(content)
        logger.warn("update method code: $code")
        return code
    }

    private fun needUpdateMethodOfController(
        targetEndpoint: String,
        clazz: DtClass,
        storyDetail: String,
        isNewController: Boolean
    ): String {
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

        val promptText =
            promptTemplate.createOrUpdateControllerMethod(clazz, storyDetail, models, services, isNewController)
        logger.warn("needUpdateMethodForController prompt text: $promptText")
        return executePrompt(promptText)
    }

    private fun executePrompt(promptText: String): String {
        ui.add(promptText, true)

        // for answer
        ui.add(AutoDevBundle.message("devti.loading"))

        return runBlocking {
            val prompt = connector.stream(promptText)
            return@runBlocking ui.updateMessage(prompt)
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

