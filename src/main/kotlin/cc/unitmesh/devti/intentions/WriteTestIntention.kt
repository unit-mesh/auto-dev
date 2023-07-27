package cc.unitmesh.devti.intentions

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.editor.LLMCoroutineScopeService
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.llms.ConnectorFactory
import cc.unitmesh.devti.parser.parseCodeFromString
import cc.unitmesh.devti.provider.TestContextProvider
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.devti.provider.context.ChatOrigin
import com.intellij.openapi.application.WriteAction
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
import org.jetbrains.kotlin.idea.core.util.toPsiFile

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
                    var prompter = "Write unit test for following code. You MUST return code only, not explain.\n\n"

                    val creationContext = ChatCreationContext(ChatOrigin.Intention, actionType, file)
                    val chatContextItems = ChatContextProvider.collectChatContextList(project, creationContext)
                    chatContextItems.forEach {
                        prompter += it.text
                    }

                    val additionContext = testContext.relatedClass.joinToString("\n") {
                        it.toQuery()
                    }.lines().joinToString("\n") {
                        "// $it"
                    }

                    prompter += additionContext

                    prompter += """```$lang
                        |$selectedText
                        |```
                        |"""

                    // navigate to the test file
                    navigateTestFile(testContext.file, editor, project)

                    val flow: Flow<String> = ConnectorFactory.getInstance().connector(project).stream(prompter, "")
                    logger<WriteTestIntention>().warn("Prompt: $prompter")
                    LLMCoroutineScopeService.scope(project).launch {
                        val suggestion = StringBuilder()
                        flow.collect {
                            suggestion.append(it)
                        }

                        parseCodeFromString(suggestion.toString()).forEach {
                            val testFile: PsiFile = PsiManager.getInstance(project).findFile(testContext.file)!!
                            testContextProvider.insertTestMethod(testFile, project, it)
                        }
                    }
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
