package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.toolwindow.chatWithSelection
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementDescriptionUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.usageView.UsageViewTypeLocation

class ChatWithThisAction : ChatBaseAction() {
    override fun getActionType(): ChatActionType {
        return ChatActionType.CHAT
    }


    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        val caretModel = event.getData(CommonDataKeys.EDITOR)?.caretModel
        val prefixText = caretModel?.currentCaret?.selectedText ?: ""
        val language = event.getData(CommonDataKeys.PSI_FILE)?.language?.displayName ?: ""

        chatWithSelection(project, language, prefixText, getActionType())
    }

    private fun computeTitle(project: Project, psiFile: PsiFile, range: TextRange): String {
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

