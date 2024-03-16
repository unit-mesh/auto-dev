package cc.unitmesh.devti.intentions.action.task

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.context.modifier.CodeModifierProvider
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.intentions.action.AutoTestThisBaseIntention
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.util.parser.parseCodeFromString
import cc.unitmesh.devti.provider.AutoTestService
import cc.unitmesh.devti.provider.context.*
import cc.unitmesh.devti.statusbar.AutoDevStatus
import cc.unitmesh.devti.statusbar.AutoDevStatusService
import cc.unitmesh.devti.template.TemplateRender
import com.intellij.lang.LanguageCommenters
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiNameIdentifierOwner
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking

data class TestGenPromptContext(
    var lang: String = "",
    var imports: String = "",
    var frameworkedContext: String = "",
    var currentClass: String = "",
    var relatedClasses: String = "",
    var sourceCode: String = "",
    var testClassName: String = "",
    var isNewFile: Boolean = true,
)

class TestCodeGenTask(val request: TestCodeGenRequest) :
    Task.Backgroundable(request.project, AutoDevBundle.message("intentions.chat.code.test.name")) {

    private val actionType = ChatActionType.GENERATE_TEST
    private val lang = request.file.language.displayName
    private val autoTestService = AutoTestService.context(request.element)

    val commenter = LanguageCommenters.INSTANCE.forLanguage(request.file.language) ?: null
    val comment = commenter?.lineCommentPrefix ?: "//"

    val templateRender = TemplateRender("genius/code")
    val template = templateRender.getTemplate("test-gen.vm")

    override fun run(indicator: ProgressIndicator) {
        indicator.isIndeterminate = true
        indicator.fraction = 0.1
        indicator.text = AutoDevBundle.message("intentions.chat.code.test.step.prepare-context")

        AutoDevStatusService.notifyApplication(AutoDevStatus.InProgress)
        val testContext = autoTestService?.findOrCreateTestFile(request.file, request.project, request.element)
        DumbService.getInstance(request.project).waitForSmartMode()

        if (testContext == null) {
            if (autoTestService == null) {
                AutoDevStatusService.notifyApplication(AutoDevStatus.Error)
                logger.error("Could not find WriteTestService for: ${request.file}, language: $lang")
                return
            }

            AutoDevStatusService.notifyApplication(AutoDevStatus.Error)
            logger.error("Failed to create test file for: ${request.file}")
            return
        }

        indicator.text = AutoDevBundle.message("intentions.chat.code.test.step.collect-context")
        indicator.fraction = 0.3

        val testPromptContext = TestGenPromptContext()

        val creationContext =
            ChatCreationContext(ChatOrigin.Intention, actionType, request.file, listOf(), element = request.element)

        val contextItems: List<ChatContextItem> = runBlocking {
            return@runBlocking ChatContextProvider.collectChatContextList(request.project, creationContext)
        }

        testPromptContext.frameworkedContext = contextItems.joinToString("\n", transform = ChatContextItem::text)
        ReadAction.compute<Unit, Throwable> {
            if (testContext.relatedClasses.isNotEmpty()) {
                testPromptContext.relatedClasses = testContext.relatedClasses.joinToString("\n") {
                    it.format()
                }.lines().joinToString("\n") {
                    "$comment $it"
                }
            }

            testPromptContext.currentClass =
                runReadAction { testContext.currentObject }?.lines()?.joinToString("\n") {
                    "$comment $it"
                } ?: ""
        }

        testPromptContext.imports = testContext.imports.joinToString("\n") {
            "$comment $it"
        }

        testPromptContext.sourceCode = if(request.element !is PsiNameIdentifierOwner) {
            testContext.testElement?.text ?: ""
        } else {
            request.selectText
        }

        testPromptContext.isNewFile = testContext.isNewFile

        templateRender.context = testPromptContext
        val prompter = templateRender.renderTemplate(template)

        logger<AutoTestThisBaseIntention>().info("Prompt: $prompter")

        indicator.fraction = 0.8
        indicator.text = AutoDevBundle.message("intentions.request.background.process.title")

        val flow: Flow<String> = try {
            LlmFactory().create(request.project).stream(prompter, "")
        } catch (e: Exception) {
            AutoDevStatusService.notifyApplication(AutoDevStatus.Error)
            logger.error("Failed to create LLM for: $lang", e)
            return
        }

        runBlocking {
            writeTestToFile(request.project, flow, testContext)
            navigateTestFile(testContext.outputFile, request.project)
            autoTestService?.runTest(request.project, testContext.outputFile)

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

    private fun navigateTestFile(testFile: VirtualFile, project: Project) {
        ApplicationManager.getApplication().invokeLater {
            val fileEditorManager = FileEditorManager.getInstance(project)
            val editors = fileEditorManager.openFile(testFile, true)

            // If the file is already open in the editor, focus on the editor tab
            if (editors.isNotEmpty()) {
                fileEditorManager.setSelectedEditor(testFile, "text-editor")
            }
        }
    }

    companion object {
        private val logger = logger<TestCodeGenTask>()
    }
}