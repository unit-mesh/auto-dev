package cc.unitmesh.devti.intentions

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.editor.LLMCoroutineScopeService
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.intentions.editor.sendToChat
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.provider.TestContextProvider
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.devti.provider.context.ChatOrigin
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
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
                val testContext = TestContextProvider.context(lang)?.prepareTestFile(file, project, element)
                if (testContext == null) {
                    logger<WriteTestIntention>().error("Failed to create test file for: $file")
                    return@runAndWait
                }

                runBlocking {
                    var prompter = "Write test for ${testContext.file.name}, ${testContext.file.path}."

                    val creationContext = ChatCreationContext(ChatOrigin.Intention, actionType, file)
                    val chatContextItems = ChatContextProvider.collectChatContextList(project, creationContext)
                    chatContextItems.forEach {
                        prompter += it.text
                    }

                    val additionContext = testContext.relatedClass.map {
                        it.toQuery()
                    }.joinToString("\n").lines().map {
                        "// $it"
                    }.joinToString("\n")

                    prompter += additionContext

                    prompter += """```$lang
                        |$selectedText
                        |```
                        |"""


                    sendToChat(project, actionType, object : ContextPrompter() {
                        override fun displayPrompt(): String {
                            return prompter
                        }

                        override fun requestPrompt(): String {
                            return prompter
                        }
                    })
                }
            }
        }
    }
}
