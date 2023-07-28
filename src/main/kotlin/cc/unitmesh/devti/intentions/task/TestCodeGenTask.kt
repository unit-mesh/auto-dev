package cc.unitmesh.devti.intentions.task

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.editor.LLMCoroutineScopeService
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.intentions.WriteTestIntention
import cc.unitmesh.devti.llms.ConnectorFactory
import cc.unitmesh.devti.parser.parseCodeFromString
import cc.unitmesh.devti.provider.TestContextProvider
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
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class TestCodeGenRequest(
    val file: PsiFile,
    val element: PsiElement,
    val project: Project,
    val editor: Editor
)

class TestCodeGenTask(
    val request: TestCodeGenRequest
) : Task.Backgroundable(request.project, AutoDevBundle.message("intentions.chat.code.test.name")) {
    private val actionType = ChatActionType.WRITE_TEST
    private val lang = request.file.language.displayName
    private val testContextProvider = TestContextProvider.context(lang)

    override fun run(indicator: ProgressIndicator) {
        val testContext = testContextProvider?.findOrCreateTestFile(request.file, request.project, request.element)
        if (testContext == null) {
            logger<WriteTestIntention>().error("Failed to create test file for: ${request.file}")
            return
        }

        var prompter = if (testContext.isNewFile) {
            """Write unit test for following code. 
                                    | You MUST return code only, not explain.
                                    | You MUST use given-when-then style.
                                    | You MUST use should_xx style for test method name.
                                    | When testing controller, you MUST use MockMvc and test API only.
                                    | """.trimMargin()
        } else {
            """Write unit test for following code. 
                                    | You MUST return method code only, not java class, no explain.
                                    | You MUST use given-when-then style.
                                    | You MUST use should_xx style for test method name.
                                    | When testing controller, you MUST use MockMvc and test API only.
                                    | You MUST return start with @Test annotation.
                                    | """.trimMargin()
        }

        LLMCoroutineScopeService.scope(project).launch {
            val creationContext = ChatCreationContext(ChatOrigin.Intention, actionType, request.file)
            val chatContextItems = ChatContextProvider.collectChatContextList(request.project, creationContext)
            chatContextItems.forEach {
                prompter += it.text
            }

            prompter += "\n"

            // sometimes, we need to wait for the project to be smart or create test package,
            // so we need to run this in a smart mode, it will wait for the project to be smart
            DumbService.getInstance(request.project).runWhenSmart {
                val additionContext = testContext.relatedFiles.joinToString("\n") {
                    it.toQuery()
                }.lines().joinToString("\n") {
                    "// $it"
                }


                prompter += additionContext

                prompter += "\n```${lang.lowercase()}\n${request.element.text}\n```\n"

                prompter += if (!testContext.isNewFile) {
                    "Start writing test method code here:  \n"
                } else {
                    "Start with `import` syntax here:  \n"
                }

                LLMCoroutineScopeService.scope(project).launch {
                    val flow: Flow<String> =
                        ConnectorFactory.getInstance().connector(request.project).stream(prompter, "")

                    logger<WriteTestIntention>().warn("Prompt: $prompter")

                    writeTestToFile(request.project, flow, testContext, testContextProvider!!)

                    navigateTestFile(testContext.file, request.editor, request.project)
                }
            }
        }
    }

    private suspend fun writeTestToFile(
        project: Project,
        flow: Flow<String>,
        context: TestFileContext,
        contextProvider: TestContextProvider
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