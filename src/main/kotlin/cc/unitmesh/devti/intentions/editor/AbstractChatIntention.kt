package cc.unitmesh.devti.intentions.editor

import cc.unitmesh.devti.gui.DevtiFlowToolWindowFactory
import cc.unitmesh.devti.gui.chat.*
import cc.unitmesh.devti.prompting.PoweredPromptFormatterProvider
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase

abstract class AbstractChatIntention : IntentionAction {
    val prompt: String = "Code completion"

    abstract fun getPrompt(project: Project, elementToExplain: PsiElement?): String

    override fun startInWriteAction(): Boolean = false

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean =
        editor != null && file != null

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return

        var selectedText = editor.selectionModel.selectedText
        val elementToExplain = getElementToExplain(project, editor)

        if (selectedText == null) {
            if (elementToExplain == null) {
                return
            }
            val startOffset = elementToExplain.textRange.startOffset
            val endOffset = elementToExplain.textRange.endOffset

            editor.selectionModel.setSelection(startOffset, endOffset)
            selectedText = editor.selectionModel.selectedText
        }

        if (selectedText == null) {
            return
        }

//        val promptToUse = getPrompt(project, elementToExplain)
        val actionType = getActionType()

        val toolWindowManager =
            ToolWindowManager.getInstance(project).getToolWindow(DevtiFlowToolWindowFactory.id) ?: return
        toolWindowManager.activate {
            val chatCodingService = ChatCodingService(actionType)
            val contentPanel = ChatCodingComponent(chatCodingService)
            val contentManager = toolWindowManager.contentManager
            val content = contentManager.factory.createContent(contentPanel, chatCodingService.getLabel(), false)

            contentManager.removeAllContents(true)
            contentManager.addContent(content)
            toolWindowManager.activate {
//                val promptFormatter = IntentionPromptFormatter(promptToUse, selectedText, file.language)

                val promptFormatter = PoweredPromptFormatterProvider(actionType, selectedText, file, project)
                chatCodingService.handlePromptAndResponse(contentPanel, promptFormatter)
            }
        }
    }

    open fun getActionType() = ChatBotActionType.CODE_COMPLETE

    protected fun getElementToExplain(project: Project?, editor: Editor?): PsiElement? {
        if (project == null || editor == null) return null

        val element = PsiUtilBase.getElementAtCaret(editor) ?: return null
        val psiFile = element.containingFile

        if (InjectedLanguageManager.getInstance(project).isInjectedFragment(psiFile)) return psiFile

        val identifierOwner = PsiTreeUtil.getParentOfType(element, PsiNameIdentifierOwner::class.java)
        return identifierOwner ?: element
    }

}
