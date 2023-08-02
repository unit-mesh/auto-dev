package cc.unitmesh.devti.intentions

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.toolwindow.sendToChat
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementDescriptionUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import com.intellij.usageView.UsageViewTypeLocation

abstract class AbstractChatIntention : IntentionAction {
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

        var selectedText = editor.selectionModel.selectedText
        val elementToExplain = getElementToAction(project, editor)

        if (selectedText == null) {
            if (elementToExplain == null) {
                return
            }
            selectElement(elementToExplain, editor)
            selectedText = editor.selectionModel.selectedText
        }

        if (selectedText == null) {
            return
        }

        val actionType = getActionType()

        val prompter = ContextPrompter.prompter(file.language.displayName)
        prompter.initContext(actionType, selectedText, file, project, editor.caretModel.offset)
        sendToChat(project, actionType, prompter)
    }

    protected fun selectElement(elementToExplain: PsiElement, editor: Editor) {
        val startOffset = elementToExplain.textRange.startOffset
        val endOffset = elementToExplain.textRange.endOffset

        editor.selectionModel.setSelection(startOffset, endOffset)
    }

    /**
     * Returns the PsiElement to explain in the given project and editor.
     *
     * @param project the project in which the element resides (nullable)
     * @param editor the editor in which the element is located (nullable)
     * @return the PsiElement to explain, or null if either the project or editor is null, or if no element is found
     */
    protected open fun getElementToAction(project: Project?, editor: Editor?): PsiElement? {
        if (project == null || editor == null) return null

        val element = PsiUtilBase.getElementAtCaret(editor) ?: return null
        val psiFile = element.containingFile

        if (InjectedLanguageManager.getInstance(project).isInjectedFragment(psiFile)) return psiFile

        val identifierOwner = PsiTreeUtil.getParentOfType(element, PsiNameIdentifierOwner::class.java)
        return identifierOwner ?: element
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

    fun calculateFrontendElementToExplain(project: Project?, psiFile: PsiFile, range: TextRange): PsiElement? {
        if (project == null || !psiFile.isValid) return null

        val element = PsiUtilBase.getElementAtOffset(psiFile, range.startOffset)
        if (InjectedLanguageManager.getInstance(project).isInjectedFragment(psiFile)) {
            return psiFile
        }

        val injected = InjectedLanguageManager.getInstance(project).findInjectedElementAt(psiFile, range.startOffset)
        if (injected != null) {
            return injected.containingFile
        }

        val psiElement: PsiElement? = PsiTreeUtil.getParentOfType(element, PsiNameIdentifierOwner::class.java)
        return psiElement ?: element
    }
}

