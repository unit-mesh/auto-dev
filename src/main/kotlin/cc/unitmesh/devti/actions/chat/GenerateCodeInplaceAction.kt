package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.editor.LLMCoroutineScopeService
import cc.unitmesh.devti.editor.inlay.InlayComponent
import cc.unitmesh.devti.editor.inlay.InlayComponent.Companion.add
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.llms.ConnectorFactory
import cc.unitmesh.devti.parser.Code
import cc.unitmesh.devti.provider.builtin.DefaultContextPrompter
import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.devti.provider.context.ChatOrigin
import com.intellij.ml.llm.core.chat.ui.editor.AIInplacePrompt
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtilEx
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.InlayProperties
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.psi.PsiFile
import com.intellij.util.DocumentUtil
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.asJava.classes.runReadAction
import org.jetbrains.kotlin.idea.util.application.invokeLater
import java.awt.event.ActionEvent
import java.util.concurrent.atomic.AtomicReference
import javax.swing.AbstractAction
import javax.swing.KeyStroke

class GenerateCodeInplaceAction : AnAction() {
    private val currentPromptInlayRef = AtomicReference<Inlay<InlayComponent<AIInplacePrompt>>?>(null)
    var currentPromptInlay: Inlay<InlayComponent<AIInplacePrompt>>?
        get() = currentPromptInlayRef.get()
        set(inlay) {
            val disposable: Disposable? = currentPromptInlayRef.getAndSet(inlay)
            if (disposable != null) {
                Disposer.dispose(disposable)
            }
        }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val dataContext = e.dataContext
        val editor = dataContext.getData(CommonDataKeys.EDITOR) ?: return
        val sourceFile = dataContext.getData(CommonDataKeys.PSI_FILE)
        val offset = editor.caretModel.offset
        val showAbove = InlayProperties().showAbove(true)
        val promptInlay: Inlay<InlayComponent<AIInplacePrompt>>? = add(editor, offset, showAbove, AIInplacePrompt())
        currentPromptInlay = promptInlay
        promptInlay?.let { setupPrompt(it, sourceFile) }
    }

    fun getChatCreationContext(list: List<ChatContextItem>, sourceFile: PsiFile?): ChatCreationContext {
        return ChatCreationContext(ChatOrigin.Unknown, ChatActionType.CUSTOM_COMPLETE, sourceFile, list)
    }

    private fun setupPrompt(inlay: Inlay<InlayComponent<AIInplacePrompt>>, sourceFile: PsiFile?) {
        val component = inlay.renderer.component
        val inputMap = component.inputMap
        val project = inlay.editor.project ?: return

        inputMap.put(KeyStroke.getKeyStroke(10, 0), PROMPT_SUBMIT_ACTION)
        inputMap.put(KeyStroke.getKeyStroke(27, 0), PROMPT_CANCEL_ACTION)

        val actionMap = component.actionMap
        actionMap.put(PROMPT_SUBMIT_ACTION, object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                val promptInlay = currentPromptInlay ?: return
                val text = component.getText()

                val chatCreationContext: ChatCreationContext = getChatCreationContext(emptyList(), sourceFile)
                this@GenerateCodeInplaceAction.send(promptInlay.editor, promptInlay.offset, text, chatCreationContext)
                currentPromptInlay = null
            }
        })
        actionMap.put(PROMPT_CANCEL_ACTION, object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                currentPromptInlay = null
            }
        })
        IdeFocusManager.getInstance(project).requestFocus(component, false)
    }

    fun send(editor: Editor, offset: Int, text: String?, chatCreationContext: ChatCreationContext?) {
        val project = editor.project ?: return
        val addCodeSnippetInlay: Inlay<InlayComponent<AIInplaceGeneratedCodeSnippet>> =
            addCodeSnippetInlay(editor, offset) ?: return

        val snippet = addCodeSnippetInlay.renderer.component
        snippet.addAcceptCallback {
            val it: String? = snippet.code
            if (it != null) {
                insertAtCaret(project, editor, it)
            }
            Disposer.dispose(addCodeSnippetInlay)
        }

        snippet.addSpecifyCallback {
            Disposer.dispose(addCodeSnippetInlay)
        }
        snippet.addRegenerateCallback {
            Disposer.dispose(addCodeSnippetInlay)
        }

        snippet.addCancelCallback {
            Disposer.dispose(addCodeSnippetInlay)
        }

        LLMCoroutineScopeService.scope(project).launch {
            val prompter = DefaultContextPrompter()
            prompter.initContext(
                chatCreationContext!!.action,
                text!!,
                chatCreationContext.sourceFile,
                project,
                ReadAction.compute<Int, Throwable> { editor.caretModel.offset }
            )
            val prompt = text + "\n" + prompter.collectionContext(chatCreationContext)

            logger<GenerateCodeInplaceAction>().warn("Result: $prompt")

            val stringFlow = ConnectorFactory().connector(project).stream(prompt, "")

            var result = ""
            stringFlow.collect { result += it }

            logger<GenerateCodeInplaceAction>().warn("Result: $result")
            ApplicationManager.getApplication().invokeLater {
                val code = Code(chatCreationContext.sourceFile!!.language, result, true)
                snippet.setCode(code, project, Disposer.newDisposable())
            }
        }
    }

    private fun insertAtCaret(project: Project, editor: Editor, text: String) {
        WriteCommandAction.runWriteCommandAction(project) {
            EditorModificationUtilEx.insertStringAtCaret(editor, text, false, true)
        }
    }

    private fun addCodeSnippetInlay(
        editor: Editor,
        offset: Int
    ): Inlay<InlayComponent<AIInplaceGeneratedCodeSnippet>>? {
        val codeSnippet = AIInplaceGeneratedCodeSnippet()
        val showAbove = InlayProperties().showAbove(true)
        return add(editor, offset, showAbove, codeSnippet)
    }

    companion object {
        const val PROMPT_CANCEL_ACTION = "ai.prompt.cancel"
        const val PROMPT_SUBMIT_ACTION = "ai.prompt.submit"
    }
}
