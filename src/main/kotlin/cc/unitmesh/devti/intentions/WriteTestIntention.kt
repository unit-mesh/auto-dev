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
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
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

        val selectedText = element.text

        val actionType = ChatActionType.WRITE_TEST

        val lang = file.language.displayName

        LLMCoroutineScopeService.scope(project).launch {
            WriteAction.runAndWait<Throwable> {
                val testContextProvider = TestContextProvider.context(lang)
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

                    val additionContext = testContext.relatedClass.joinToString("\n") {
                        it.toQuery()
                    }.lines().joinToString("\n") {
                        "// $it"
                    }

                    prompter += additionContext

                    prompter += "```$lang\n$selectedText\n```\n"

                    if (!testContext.isNewFile) {
                        prompter += "Start writing test method code here:  \n"
                    }

                    // navigate to the test file
                    navigateTestFile(testContext.file, editor, project)

                    val flow: Flow<String> = ConnectorFactory.getInstance().connector(project).stream(prompter, "")
                    logger<WriteTestIntention>().warn("Prompt: $prompter")
                    writeTestToFile(project, flow, testContext, testContextProvider)
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

            runReadAction {
                parseCodeFromString(suggestion.toString()).forEach {
                    val testFile: PsiFile = PsiManager.getInstance(project).findFile(context.file)!!
                    contextProvider.insertTestCode(testFile, project, it)
                }
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
