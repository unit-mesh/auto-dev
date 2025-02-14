package cc.unitmesh.idea.prompting

import cc.unitmesh.devti.custom.action.CustomPromptConfig
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.gui.chat.message.GenApiTestContext
import cc.unitmesh.devti.prompting.TextTemplatePrompt
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.provider.PsiElementDataBuilder
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.devti.provider.context.ChatOrigin
import cc.unitmesh.devti.settings.AutoDevSettingsState
import cc.unitmesh.idea.MvcUtil
import cc.unitmesh.idea.flow.MvcContextService
import cc.unitmesh.idea.provider.JavaPsiElementDataBuilder
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.temporary.similar.chunks.SimilarChunksWithPaths
import kotlinx.coroutines.runBlocking

open class JavaContextPrompter : ContextPrompter() {
    private val logger = logger<JavaContextPrompter>()
    private var additionContext: String = ""
    protected open val psiElementDataBuilder: PsiElementDataBuilder = JavaPsiElementDataBuilder()

    private val autoDevSettingsState = AutoDevSettingsState.getInstance()
    private var customPromptConfig: CustomPromptConfig? = null
    private lateinit var mvcContextService: MvcContextService
    private var fileName = ""
    private lateinit var creationContext: ChatCreationContext

    override fun appendAdditionContext(context: String) {
        additionContext += context
    }

    override fun initContext(
        actionType: ChatActionType,
        selectedText: String,
        file: PsiFile?,
        project: Project,
        offset: Int,
        element: PsiElement?,
    ) {
        super.initContext(actionType, selectedText, file, project, offset, element)
        mvcContextService = MvcContextService(project)

        lang = file?.language?.displayName ?: ""
        fileName = file?.name ?: ""
        creationContext = ChatCreationContext(ChatOrigin.ChatAction, action!!, file, listOf(), element)
    }

    init {
        val prompts = autoDevSettingsState.customPrompts
        customPromptConfig = CustomPromptConfig.tryParse(prompts)
    }

    override fun displayPrompt(): String {
        val instruction = createPrompt(selectedText).displayText

        val finalPrompt = if (additionContext.isNotEmpty()) {
            "```\n$additionContext\n```\n```$lang\n$selectedText\n```\n"
        } else {
            "```$lang\n$selectedText\n```"
        }

        return "$instruction: \n$finalPrompt"
    }

    override fun requestPrompt(): String {
        return runBlocking {
            val instruction = createPrompt(selectedText)
            val chatContext = collectionContext(creationContext)

            var finalPrompt = instruction.requestText

            if (chatContext.isNotEmpty()) {
                finalPrompt += "\n$chatContext"
            }

            if (additionContext.isNotEmpty()) {
                finalPrompt += "\n$additionContext"
            }

            finalPrompt += "```$lang\n$selectedText\n```"

            logger.info("final prompt: $finalPrompt")
            return@runBlocking finalPrompt
        }
    }


    private fun createPrompt(selectedText: String): TextTemplatePrompt {
        additionContext = ""
        val prompt = action!!.instruction(lang, project)

        when (action!!) {
            ChatActionType.CODE_COMPLETE -> {
                when {
                    MvcUtil.isController(fileName, lang) -> {
                        val spec = CustomPromptConfig.load().spec["controller"]
                        if (!spec.isNullOrEmpty()) {
                            additionContext = "requirements: \n$spec"
                        }
                        additionContext += mvcContextService.controllerPrompt(file)
                    }

                    MvcUtil.isService(fileName, lang) -> {
                        val spec = CustomPromptConfig.load().spec["service"]
                        if (!spec.isNullOrEmpty()) {
                            additionContext = "requirements: \n$spec"
                        }
                        additionContext += mvcContextService.servicePrompt(file)
                    }

                    else -> {
                        additionContext = SimilarChunksWithPaths.createQuery(file!!) ?: ""
                    }
                }
            }
            ChatActionType.FIX_ISSUE -> addFixIssueContext(selectedText)
            ChatActionType.GENERATE_TEST_DATA -> prepareDataStructure(creationContext, action!!)
            else -> {
                // ignore else
            }
        }

        return prompt.renderTemplate()
    }

    open fun prepareDataStructure(creationContext: ChatCreationContext, action: ChatActionType) {
        val element = creationContext.element ?: return logger.error("element is null")
        var baseUri = ""
        var requestBody = ""
        var relatedClasses = ""

        psiElementDataBuilder.baseRoute(element).let {
            baseUri = it
        }

        psiElementDataBuilder.inboundData(element).forEach { (_, value) ->
            requestBody = value
        }
        psiElementDataBuilder.outboundData(element).forEach { (_, value) ->
            relatedClasses = value
        }

        if (action == ChatActionType.GENERATE_TEST_DATA) {
            (action.context as GenApiTestContext).baseUri = baseUri
            (action.context as GenApiTestContext).requestBody = requestBody
            (action.context as GenApiTestContext).relatedClasses = relatedClasses.split(",")
        }
    }

    private fun addFixIssueContext(selectedText: String) {
        val projectPath = project!!.basePath ?: ""
        runReadAction {
            val lookupFile = if (selectedText.contains(projectPath)) {
                val regex = Regex("$projectPath(.*\\.)${lang.lowercase()}")
                val relativePath = regex.find(selectedText)?.groupValues?.get(1) ?: ""
                val file = LocalFileSystem.getInstance().findFileByPath(projectPath + relativePath)
                file?.let { PsiManager.getInstance(project!!).findFile(it) }
            } else {
                null
            }

            if (lookupFile != null) {
                additionContext = lookupFile.text.toString()
            }
        }
    }
}
