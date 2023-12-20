package cc.unitmesh.devti.intentions.action.task

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.context.modifier.CodeModifierProvider
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.intentions.action.AutoTestThisIntention
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.parser.parseCodeFromString
import cc.unitmesh.devti.provider.WriteTestService
import cc.unitmesh.devti.provider.context.*
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
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking

class TestCodeGenTask(val request: TestCodeGenRequest) :
    Task.Backgroundable(request.project, AutoDevBundle.message("intentions.chat.code.test.name")) {

    private val actionType = ChatActionType.GENERATE_TEST
    private val lang = request.file.language.displayName
    private val writeTestService = WriteTestService.context(request.element)

    override fun run(indicator: ProgressIndicator) {
        indicator.isIndeterminate = true
        indicator.fraction = 0.1
        indicator.text = AutoDevBundle.message("intentions.chat.code.test.step.prepare-context")

        val testContext = writeTestService?.findOrCreateTestFile(request.file, request.project, request.element)
        DumbService.getInstance(request.project).waitForSmartMode()

        if (testContext == null) {
            if (writeTestService == null) {
                logger.error("Could not find WriteTestService for: ${request.file}")
                return
            }

            logger.error("Failed to create test file for: ${request.file}")
            return
        }

        var prompter = "Write unit test for following code. "

        indicator.text = AutoDevBundle.message("intentions.chat.code.test.step.collect-context")
        indicator.fraction = 0.3

        val creationContext =
            ChatCreationContext(ChatOrigin.Intention, actionType, request.file, listOf(), element = request.element)

        val contextItems: List<ChatContextItem> = runBlocking {
            return@runBlocking ChatContextProvider.collectChatContextList(request.project, creationContext)
        }

        contextItems.forEach {
            prompter += it.text + "\n"
        }

        prompter += "\n"
        prompter += ReadAction.compute<String, Throwable> {
            if (testContext.relatedClasses.isEmpty()) {
                return@compute ""
            }

            val relatedClasses = testContext.relatedClasses.joinToString("\n") {
                it.format()
            }.lines().joinToString("\n") {
                "// $it"
            }

            "// here are related classes:\n$relatedClasses"
        }

        if (testContext.currentClass != null) {
            prompter += "\n"
            prompter += "// here is current class information:\n"
            prompter += runReadAction { testContext.currentClass.format() }
        }

        prompter += "\n```${lang.lowercase()}\nCode:\n${request.selectText}\n```\n"
        prompter += if (!testContext.isNewFile) {
            "Start test code with `@Test` syntax here:  \n"
        } else {
            "Start ${testContext.testClassName} with `import` syntax here:  \n"
        }

        val flow: Flow<String> =
            LlmFactory().create(request.project).stream(prompter, "")

        logger<AutoTestThisIntention>().info("Prompt: $prompter")

        indicator.fraction = 0.8
        indicator.text = AutoDevBundle.message("intentions.request.background.process.title")

        runBlocking {
            writeTestToFile(request.project, flow, testContext)
            navigateTestFile(testContext.file, request.project)
            writeTestService?.runTest(request.project, testContext.file)
            indicator.fraction = 1.0
        }
    }

    @OptIn(InternalCoroutinesApi::class)
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

        parseCodeFromString(suggestion.toString()).forEach {
            modifier.insertTestCode(context.file, project, it)
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