package cc.unitmesh.devti.intentions.task

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.editor.LLMCoroutineScopeService
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.intentions.WriteTestIntention
import cc.unitmesh.devti.llms.ConnectorFactory
import cc.unitmesh.devti.parser.parseCodeFromString
import cc.unitmesh.devti.provider.WriteTestService
import cc.unitmesh.devti.provider.TestFileContext
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.devti.provider.context.ChatOrigin
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.jvm.internal.Ref

class TestCodeGenRequest(
    val file: PsiFile,
    val element: PsiElement,
    val project: Project,
    val editor: Editor,
    val selectText: String
)

class TestCodeGenTask(
    val request: TestCodeGenRequest
) : Task.Backgroundable(request.project, AutoDevBundle.message("intentions.chat.code.test.name")) {
    private val actionType = ChatActionType.WRITE_TEST
    private val lang = request.file.language.displayName
    private val writeTestService = WriteTestService.context(lang)

    override fun run(indicator: ProgressIndicator) {
        indicator.isIndeterminate = true
        indicator.fraction = 0.1
        indicator.text = AutoDevBundle.message("intentions.chat.code.test.step.prepare-context")

        val testContext = writeTestService?.findOrCreateTestFile(request.file, request.project, request.element)
        if (testContext == null) {
            logger<WriteTestIntention>().error("Failed to create test file for: ${request.file}")
            return
        }

        var prompter = if (testContext.isNewFile) {
            """Write unit test for following code. 
                                    | You MUST return code only, not explain.
                                    | """.trimMargin()
        } else {
            """Write unit test for following code. 
                                    | You MUST return method code only, no explain.
                                    | You MUST return start with @Test annotation.
                                    | """.trimMargin()
        }

        LLMCoroutineScopeService.scope(project).launch {
            val creationContext = ChatCreationContext(ChatOrigin.Intention, actionType, request.file)
            val chatContextItems = ChatContextProvider.collectChatContextList(request.project, creationContext)
            chatContextItems.forEach {
                prompter += it.text
            }

            indicator.text = AutoDevBundle.message("intentions.chat.code.test.step.collect-context")
            indicator.fraction = 0.3

            prompter += "\n"

            val additionContextRef: Ref.ObjectRef<String> = Ref.ObjectRef()
            additionContextRef.element = ""
            ApplicationManager.getApplication().runReadAction {
                additionContextRef.element = testContext.relatedFiles.joinToString("\n") {
                    it.toQuery()
                }.lines().joinToString("\n") {
                    "// $it"
                }
            }

            prompter += additionContextRef.element

            prompter += "\n```${lang.lowercase()}\n${request.selectText}\n```\n"

            prompter += if (!testContext.isNewFile) {
                "Start test code with `@Test` syntax here:  \n"
            } else {
                "Start ${testContext.testClassName} with `import` syntax here:  \n"
            }

            indicator.fraction = 0.8
            indicator.text = AutoDevBundle.message("intentions.chat.code.test.step.prompt")

            val flow: Flow<String> =
                ConnectorFactory.getInstance().connector(request.project).stream(prompter, "")

            logger<WriteTestIntention>().warn("Prompt: $prompter")

            indicator.fraction = 0.9
            indicator.text = AutoDevBundle.message("intentions.chat.code.test.step.write-test")

            writeTestToFile(request.project, flow, testContext, writeTestService!!)

            navigateTestFile(testContext.file, request.editor, request.project)

            writeTestService.runJunitTest(request.project, testContext.file)

            indicator.fraction = 1.0
        }
    }

    private suspend fun writeTestToFile(
        project: Project,
        flow: Flow<String>,
        context: TestFileContext,
        contextProvider: WriteTestService
    ) {
        val suggestion = StringBuilder()
        flow.collect {
            suggestion.append(it)
        }

        logger<WriteTestIntention>().warn("LLM suggestion: $suggestion")

        parseCodeFromString(suggestion.toString()).forEach {
            contextProvider.insertTestCode(context.file, project, it)
        }
    }

    fun navigateTestFile(testFile: VirtualFile, editor: Editor, project: Project) {
        ApplicationManager.getApplication().invokeLater {
            val fileEditorManager = FileEditorManager.getInstance(project)
            val editors = fileEditorManager.openFile(testFile, true)

            // If the file is already open in the editor, focus on the editor tab
            if (editors.isNotEmpty()) {
                fileEditorManager.setSelectedEditor(testFile, "text-editor")
            }
        }
    }
}