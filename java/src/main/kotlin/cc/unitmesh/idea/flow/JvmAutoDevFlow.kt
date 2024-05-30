package cc.unitmesh.idea.flow

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.context.model.DtClass
import cc.unitmesh.devti.flow.kanban.Kanban
import cc.unitmesh.devti.flow.model.SimpleStory
import cc.unitmesh.devti.prompting.code.TargetEndpoint
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import cc.unitmesh.devti.llms.LLMProvider
import cc.unitmesh.devti.flow.PromptTemplate
import cc.unitmesh.devti.util.parser.parseCodeFromString
import cc.unitmesh.devti.provider.DevFlowProvider
import cc.unitmesh.devti.provider.PromptStrategy
import cc.unitmesh.devti.runconfig.AutoDevRunProfileState
import cc.unitmesh.idea.fromJavaFile
import cc.unitmesh.idea.spring.JavaSpringCodeCreator
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import kotlinx.coroutines.runBlocking

class JvmAutoDevFlow : DevFlowProvider() {
    private val promptTemplate = PromptTemplate()
    private var selectedControllerName = ""
    private var selectedControllerCode = ""
    private var isNewController = false

    private var kanban: Kanban? = null
    private lateinit var connector: LLMProvider
    private lateinit var ui: ChatCodingPanel
    private lateinit var processor: JavaSpringCodeCreator
    private lateinit var promptStrategy: PromptStrategy

    override fun initContext(
        kanban: Kanban?,
        aiRunner: LLMProvider,
        component: ChatCodingPanel,
        project: Project,
    ) {
        this.kanban = kanban
        this.connector = aiRunner
        this.ui = component
        processor = project.service<JavaSpringCodeCreator>()
        promptStrategy = PromptStrategy.strategy("java")!!
    }

    /**
     * Step 1: check story detail is valid, if not, fill story detail
     */
    override fun getOrCreateStoryDetail(id: String): String {
        val simpleProject = kanban?.getProjectInfo() ?: return ""
        val story = kanban!!.getStoryById(id)

        // 1. check story detail is valid, if not, fill story detail
        var storyDetail = story.description
        if (!kanban!!.isValidStory(storyDetail)) {
            logger.info("story detail is not valid, fill story detail")

            storyDetail = run {
                val promptText = promptTemplate.storyDetail(simpleProject, story.description)
                executePrompt(promptText)
            }

            val newStory = SimpleStory(story.id, story.title, storyDetail)
            kanban!!.updateStoryDetail(newStory)
        }

        logger.info("user story detail: $storyDetail")
        return storyDetail
    }

    /**
     * Step 2: base on story detail, generate dto and entity
     */
    override fun updateOrCreateDtoAndEntity(storyDetail: String) {
        val files: List<DtClass> = processor.getAllModelFiles()
        val promptText = promptTemplate.createDtoAndEntity(storyDetail, files)

        logger.info("needUpdateMethodForController prompt text: $promptText")
        val result = executePrompt(promptText)

        parseCodeFromString(result).forEach { dto ->
            createCodeByType(dto)
        }
    }

    /**
     * Step 3: fetch suggest endpoint, if not found, return null
     */
    override fun fetchSuggestEndpoint(storyDetail: String): TargetEndpoint {
        val files: List<DtClass> = processor.getAllControllerFiles().map(DtClass.Companion::fromJavaFile)
        logger.info("start devti flow")
        val promptText = promptTemplate.createEndpoint(storyDetail, files)
        val targetEndpoint = executePrompt(promptText)

        val controller = matchControllerName(targetEndpoint)
        if (controller == null) {
            logger.info("no controller found from: $controller")
            return TargetEndpoint("", DtClass("", listOf()), false)
        }

        logger.info("target endpoint: $controller")
        val targetController = files.find { it.name == controller }
        if (targetController == null) {
            isNewController = true
            logger.info("no controller found from: $controller")
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
            logger.info("update method failed: $e, try to fill update method 2nd")
            doExecuteUpdateEndpoint(target, storyDetail, isNewController)
        }
    }

    /**
     * Step 5: create service and repository
     */
    override fun updateOrCreateServiceAndRepository() {
        val serviceName = selectedControllerName.removeSuffix("Controller") + "Service"
        val files: List<PsiFile> = processor.getAllServiceFiles().filter { it.name == "$serviceName.java" }

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
        val noExistMethods = JavaCodeProcessor.findNoExistMethod(serviceFile, usedCode)
        if (noExistMethods.isEmpty()) {
            logger.info("no need to update service method")
            return
        }

        // 3. if serviceFile not exist used method, send service code to openai
        val finalPrompt = promptStrategy.advice(serviceFile, usedCode, noExistMethods)
        val promptText = promptTemplate.updateServiceMethod(finalPrompt)
        val result = executePrompt(promptText)
        val services = parseCodeFromString(result)
        services.forEach { code ->
            processor.updateMethod(serviceFile, serviceName, code)
        }
    }

    private fun createServiceFile(serviceName: String) {
        val controllerFile: List<PsiFile> =
            processor.getAllControllerFiles().filter { it.name == selectedControllerName }

        val controllerCode = if (controllerFile.isEmpty()) {
            selectedControllerCode
        } else {
            runReadAction {
                promptStrategy.advice(controllerFile.first(), serviceName).prefixCode
            }
        }

        val promptText = promptTemplate.createServiceAndRepository(controllerCode)

        logger.info("createServiceAndController prompt text: $promptText")
        val result = executePrompt(promptText)

        val services = parseCodeFromString(result)
        services.forEach { service ->
            createCodeByType(service, false)
        }
    }


    private fun doExecuteUpdateEndpoint(target: TargetEndpoint, storyDetail: String, isNewController: Boolean) {
        val codes = fetchForEndpoint(target.endpoint, target.controller, storyDetail, isNewController)
        if (codes.isEmpty()) {
            logger.info("update method code is empty, skip")
        } else {
            codes.indices.forEach { i ->
                createCodeByType(codes[i], target.isNeedToCreated, target.controller.name)
            }
        }
    }

    private fun createCodeByType(code: String, isNeedCreateController: Boolean = false, controllerName: String = "") {
        JavaParseUtil.splitClass(code).forEach {
            createCode(it, controllerName, isNeedCreateController)
        }
    }

    private fun createCode(code: String, controllerName: String, isNeedCreateController: Boolean, ) {
        when {
            isNeedCreateController || processor.isController(code) -> {
                selectedControllerCode = code
                processor.createControllerOrUpdateMethod(controllerName, code, isNeedCreateController)
            }

            processor.isService(code) -> {
                processor.createService(code)
            }

            processor.isDto(code) -> {
                processor.createDto(code)
            }

            processor.isEntity(code) -> {
                processor.createEntity(code)
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
        logger.info("update method code: $code")
        return code
    }

    private fun needUpdateMethodOfController(
        targetEndpoint: String,
        clazz: DtClass,
        storyDetail: String,
        isNewController: Boolean,
    ): String {
        val allModels = processor.getAllModelFiles().map { it }
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

        val services = processor.getAllServiceFiles().map(DtClass.Companion::fromJavaFile)

        val promptText =
            promptTemplate.createOrUpdateControllerMethod(clazz, storyDetail, models, services, isNewController)
        logger.info("needUpdateMethodForController prompt text: $promptText")
        return executePrompt(promptText)
    }

    private fun executePrompt(promptText: String): String {
        ui.addMessage(promptText, false, promptText)
        ui.addMessage(AutoDevBundle.message("autodev.loading"))

        return runBlocking {
            val prompt = connector.stream(promptText, "")
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

