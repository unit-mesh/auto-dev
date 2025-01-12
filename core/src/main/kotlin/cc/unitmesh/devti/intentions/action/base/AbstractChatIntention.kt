package cc.unitmesh.devti.intentions.action.base

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.temporary.calculateFrontendElementToExplain
import com.intellij.temporary.getElementToAction
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.gui.sendToChatPanel
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementDescriptionUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.usageView.UsageViewTypeLocation

abstract class AbstractChatIntention : IntentionAction {
    abstract fun priority(): Int
    open fun getActionType() = ChatActionType.CODE_COMPLETE

    open val prompt: String = "Code completion"

    override fun startInWriteAction(): Boolean = false

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean =
        editor != null && file != null

    /**
     * Invokes the given method with the specified parameters.
     *
     * @param project The current project.
     * @param editor The editor in which the method is invoked.
     * @param file The file in which the method is invoked.
     */
    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return
        val withRange = elementWithRange(editor, file, project) ?: return

        val selectedText = withRange.first
        val psiElement = withRange.second

        val actionType = getActionType()

        val prompter = ContextPrompter.prompter(file.language.displayName)
        prompter.initContext(actionType, selectedText, file, project, editor.caretModel.offset, psiElement)
        sendToChatPanel(project, actionType, prompter)
    }

    fun elementWithRange(
        editor: Editor,
        file: PsiFile,
        project: Project,
    ): Pair<@NlsSafe String, PsiElement?>? {
        var selectedText = editor.selectionModel.selectedText
        val psiElement = getElementToAction(project, editor)

        if (selectedText == null) {
            if (psiElement == null) {
                return null
            }
            selectElement(psiElement, editor)
            selectedText = editor.selectionModel.selectedText
        }

        if (selectedText == null) {
            return null
        }

        return Pair(selectedText, psiElement)
    }

    protected fun selectElement(elementToExplain: PsiElement, editor: Editor) {
        val startOffset = elementToExplain.textRange.startOffset
        val endOffset = elementToExplain.textRange.endOffset

        editor.selectionModel.setSelection(startOffset, endOffset)
    }

    fun getCurrentSelectionAsRange(editor: Editor): TextRange {
        val currentCaret = editor.caretModel.currentCaret
        return TextRange(currentCaret.selectionStart, currentCaret.selectionEnd)
    }

    fun computeTitle(project: Project, psiFile: PsiFile, range: TextRange): String {
        val defaultTitle = AutoDevBundle.message("intentions.chat.selected.code.name")
        if (!range.isEmpty) {
            return defaultTitle
        }
        val element: PsiElement = calculateFrontendElementToExplain(project, psiFile, range) ?: return defaultTitle

        return when {
            element is PsiFile -> {
                if (InjectedLanguageManager.getInstance(project).isInjectedFragment(element)) {
                    val displayName = element.getLanguage().displayName
                    return AutoDevBundle.message("intentions.chat.selected.fragment.name", displayName)
                }

                val name: String = element.name
                return AutoDevBundle.message("intentions.chat.selected.element.name", name, getDescription(element))
            }

            element is PsiNameIdentifierOwner && element.name != null -> {
                AutoDevBundle.message("intentions.chat.selected.element.name", element.name!!, getDescription(element))
            }

            else -> {
                defaultTitle
            }
        }
    }

    private fun getDescription(element: PsiElement): String {
        return ElementDescriptionUtil.getElementDescription(element, UsageViewTypeLocation.INSTANCE)
    }
}
