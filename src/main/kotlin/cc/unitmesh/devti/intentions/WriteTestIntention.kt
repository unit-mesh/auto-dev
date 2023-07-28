package cc.unitmesh.devti.intentions

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.editor.LLMCoroutineScopeService
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.llms.ConnectorFactory
import cc.unitmesh.devti.parser.parseCodeFromString
import cc.unitmesh.devti.provider.TestContextProvider
import cc.unitmesh.devti.provider.TestFileContext
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.devti.provider.context.ChatOrigin
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class WriteTestIntention : AbstractChatIntention() {
    override fun getText(): String = AutoDevBundle.message("intentions.chat.code.test.name")
    override fun getFamilyName(): String = AutoDevBundle.message("intentions.chat.code.test.family.name")

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return

        val element = getElementToAction(project, editor) ?: return
        selectElement(element, editor)

        LLMCoroutineScopeService.scope(project).launch {
            writeTestTask(file, element, project, editor)
        }
    }

    private fun writeTestTask(file: PsiFile, element: PsiElement, project: Project, editor: Editor) {
        val lang = file.language.displayName

        val selectedText = element.text
        val actionType = ChatActionType.WRITE_TEST

        val testContextProvider = TestContextProvider.context(lang)

        WriteAction.runAndWait<Throwable> {
            val testContext = testContextProvider?.findOrCreateTestFile(file, project, element)
            if (testContext == null) {
                logger<WriteTestIntention>().error("Failed to create test file for: $file")
                return@runAndWait
            }

            runBlocking {
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

                val creationContext = ChatCreationContext(ChatOrigin.Intention, actionType, file)
                val chatContextItems = ChatContextProvider.collectChatContextList(project, creationContext)
                chatContextItems.forEach {
                    prompter += it.text
                }

                prompter += "\n"

                // sometimes, we need to wait for the project to be smart or create test package,
                // so we need to run this in a smart mode, it will wait for the project to be smart
                DumbService.getInstance(project).runWhenSmart {
                    val additionContext = testContext.relatedClass.joinToString("\n") {
                        it.toQuery()
                    }.lines().joinToString("\n") {
                        "// $it"
                    }


                    prompter += additionContext

                    prompter += "\n```${lang.lowercase()}\n$selectedText\n```\n"

                    prompter += if (!testContext.isNewFile) {
                        "Start writing test method code here:  \n"
                    } else {
                        "Start with `import` syntax here:  \n"
                    }

                    val flow: Flow<String> = ConnectorFactory.getInstance().connector(project).stream(prompter, "")
                    logger<WriteTestIntention>().warn("Prompt: $prompter")
                    writeTestToFile(project, flow, testContext, testContextProvider)

                    // navigate to the test file
                    navigateTestFile(testContext.file, editor, project)
                }
            }
        }
    }

    private fun writeTestToFile(
        project: Project,
        flow: Flow<String>,
        context: TestFileContext,
        contextProvider: TestContextProvider
    ) {
        LLMCoroutineScopeService.scope(project).launch {
            val suggestion = StringBuilder()
            flow.collect {
                suggestion.append(it)
            }

            logger<WriteTestIntention>().warn("LLM suggestion: $suggestion")

            parseCodeFromString(suggestion.toString()).forEach {
                contextProvider.insertTestCode(context.file, project, it)
            }
        }
    }

    private fun navigateTestFile(testFile: VirtualFile, editor: Editor, project: Project) {
        val fileEditorManager = FileEditorManager.getInstance(project)
        val editors = fileEditorManager.openFile(testFile, true)

        // If the file is already open in the editor, focus on the editor tab
        if (editors.isNotEmpty()) {
            fileEditorManager.setSelectedEditor(testFile, "text-editor")
        }
    }
}
