package cc.unitmesh.devti.intentions.action.task

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.agent.CustomAgentExecutor
import cc.unitmesh.devti.agent.configurable.customAgentSetting
import cc.unitmesh.devti.agent.model.CustomAgentConfig
import cc.unitmesh.devti.context.modifier.CodeModifierProvider
import cc.unitmesh.devti.custom.CustomExtContext
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.intentions.action.test.TestCodeGenContext
import cc.unitmesh.devti.intentions.action.test.TestCodeGenRequest
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.provider.AutoTestService
import cc.unitmesh.devti.provider.context.*
import cc.unitmesh.devti.statusbar.AutoDevStatus
import cc.unitmesh.devti.statusbar.AutoDevStatusService
import cc.unitmesh.devti.template.GENIUS_CODE
import cc.unitmesh.devti.template.TemplateRender
import cc.unitmesh.devti.util.parser.parseCodeFromString
import com.intellij.lang.LanguageCommenters
import com.intellij.openapi.application.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking

class TestCodeGenTask(val request: TestCodeGenRequest, displayMessage: String) :
    Task.Backgroundable(request.project, displayMessage) {
    private val logger = logger<TestCodeGenTask>()
    private val actionType = ChatActionType.GENERATE_TEST
    private val lang = request.file.language.displayName

    private val commenter = LanguageCommenters.INSTANCE.forLanguage(request.file.language) ?: null
    private val comment = commenter?.lineCommentPrefix ?: "//"

    private val templateRender = TemplateRender(GENIUS_CODE)
    private val template = templateRender.getTemplate("test-gen.vm")

    override fun run(indicator: ProgressIndicator) {
        val autoTestService = AutoTestService.context(request.element) ?: return

        indicator.isIndeterminate = false
        indicator.fraction = 0.1
        indicator.text = AutoDevBundle.message("intentions.chat.code.test.step.prepare-context")

        AutoDevStatusService.notifyApplication(AutoDevStatus.InProgress)
        val testContext = autoTestService.findOrCreateTestFile(request.file, request.project, request.element)
        DumbService.getInstance(request.project).waitForSmartMode()

        if (testContext == null) {
            AutoDevStatusService.notifyApplication(AutoDevStatus.Error)
            logger.error("Failed to create test file for: ${request.file}")
            return
        }

        indicator.text = AutoDevBundle.message("intentions.chat.code.test.step.collect-context")
        indicator.fraction = 0.3

        val testPromptContext = TestCodeGenContext()

        val creationContext =
            ChatCreationContext(ChatOrigin.Intention, actionType, request.file, listOf(), element = request.element)

        val contextItems: List<ChatContextItem> = runBlocking {
            return@runBlocking ChatContextProvider.collectChatContextList(request.project, creationContext)
        }

        testPromptContext.frameworkContext = contextItems.joinToString("\n", transform = ChatContextItem::text)
        ReadAction.compute<Unit, Throwable> {
            if (testContext.relatedClasses.isNotEmpty()) {
                testPromptContext.relatedClasses = testContext.relatedClasses.joinToString("\n") {
                    it.format()
                }.lines().joinToString("\n") {
                    "$comment $it"
                }
            }

            testPromptContext.currentClass = runReadAction { testContext.currentObject }?.lines()?.joinToString("\n") {
                "$comment $it"
            } ?: ""
        }

        testPromptContext.imports = testContext.imports.joinToString("\n") {
            "$comment $it"
        }

        testPromptContext.sourceCode = runReadAction {
            when (request.element) {
                is PsiFile -> {
                    request.element.text ?: ""
                }

                !is PsiNameIdentifierOwner -> {
                    testContext.testElement?.text ?: ""
                }

                else -> {
                    request.element.text ?: ""
                }
            }
        }

        testPromptContext.isNewFile = testContext.isNewFile
        testPromptContext.extContext = getCustomAgentTestContext(testPromptContext)

        templateRender.context = testPromptContext
        val prompter = templateRender.renderTemplate(template)

        logger.info("Prompt: $prompter")

        indicator.fraction = 0.6
        indicator.text = AutoDevBundle.message("intentions.request.background.process.title")

        val flow: Flow<String> = try {
            LlmFactory().create(request.project).stream(prompter, "", false)
        } catch (e: Exception) {
            AutoDevStatusService.notifyApplication(AutoDevStatus.Error)
            logger.error("Failed to create LLM for: $lang", e)
            return
        }

        runBlocking {
            writeTestToFile(request.project, flow, testContext)

            indicator.fraction = 1.0
            indicator.text = AutoDevBundle.message("intentions.chat.code.test.verify")

            try {
                autoTestService.collectSyntaxError(testContext.outputFile, request.project) {
                    autoTestService.tryFixSyntaxError(testContext.outputFile, request.project, it)

                    if (it.isNotEmpty()) {
                        AutoDevNotifications.warn(
                            request.project,
                            "Test has error, skip auto run test: ${it.joinToString("\n")}"
                        )
                        indicator.fraction = 1.0
                    } else {
                        autoTestService.runFile(request.project, testContext.outputFile, testContext.testElement, false)
                    }
                }
            } catch (e: Exception) {
                AutoDevStatusService.notifyApplication(AutoDevStatus.Ready)
                indicator.fraction = 1.0
            }

            AutoDevStatusService.notifyApplication(AutoDevStatus.Ready)
            indicator.fraction = 1.0
        }
    }

    override fun onThrowable(error: Throwable) {
        super.onThrowable(error)
        AutoDevStatusService.notifyApplication(AutoDevStatus.Error)
        AutoDevNotifications.error(project, "Failed to generate test: ${error.message}")
    }

    private suspend fun writeTestToFile(
        project: Project,
        flow: Flow<String>,
        context: TestFileContext,
    ) {
        val fileEditorManager = FileEditorManager.getInstance(project)
        var editors: Array<FileEditor> = emptyArray()
        ApplicationManager.getApplication().invokeAndWait {
            editors = fileEditorManager.openFile(context.outputFile, true)
        }

        if (editors.isNotEmpty()) {
            fileEditorManager.setSelectedEditor(context.outputFile, "text-editor")
        }

        val isTargetEmpty = context.outputFile.length == 0L
        if (editors.isNotEmpty() && (context.isNewFile || isTargetEmpty)) {
            val suggestion = StringBuilder()
            val editor = fileEditorManager.selectedTextEditor

            flow.collect {
                suggestion.append(it)
                val codeBlocks = parseCodeFromString(suggestion.toString())
                codeBlocks.forEach {
                    WriteCommandAction.writeCommandAction(project).compute<Any, RuntimeException> {
                        editor?.document?.replaceString(0, editor.document.textLength, it)
                        editor?.caretModel?.moveToOffset(editor.document.textLength)
                        editor?.scrollingModel?.scrollToCaret(ScrollType.RELATIVE)
                    }
                }
            }

            logger.info("LLM suggestion: $suggestion")
            return
        }

        val suggestion = StringBuilder()
        flow.collect {
            suggestion.append(it)
        }

        logger.info("LLM suggestion: $suggestion")

        val modifier = CodeModifierProvider().modifier(context.language)
            ?: throw IllegalStateException("Unsupported language: ${context.language}")

        val codeBlocks = parseCodeFromString(suggestion.toString())
        codeBlocks.forEach {
            modifier.insertTestCode(context.outputFile, project, it)
        }
    }

    private fun getCustomAgentTestContext(testPromptContext: TestCodeGenContext): String {
        if (!project.customAgentSetting.enableCustomRag) return ""

        val agent = loadTestRagConfig() ?: return ""

        val query = testPromptContext.sourceCode
        val stringFlow: Flow<String> = CustomAgentExecutor(project).execute(query, agent) ?: return ""

        val responseBuilder = StringBuilder()
        runBlocking {
            stringFlow.collect { string ->
                responseBuilder.append(string)
            }
        }

        return responseBuilder.toString()
    }

    private fun loadTestRagConfig(): CustomAgentConfig? {
        val rags = CustomAgentConfig.loadFromProject(project)
        if (rags.isEmpty()) return null

        return rags.firstOrNull { it.name == CustomExtContext.TextContext.agentName }
    }
}