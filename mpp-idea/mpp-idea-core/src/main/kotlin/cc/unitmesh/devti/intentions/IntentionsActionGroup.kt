package cc.unitmesh.devti.intentions

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import java.util.function.Supplier

class IntentionsActionGroup : ActionGroup(Supplier { AutoDevBundle.message("intentions.assistant.name") }, true), DumbAware {
    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val project: Project = e?.project ?: return emptyArray()
        val editor: Editor = e.getData(CommonDataKeys.EDITOR) ?: return emptyArray()
        val file: PsiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return emptyArray()

        val intentions: List<IntentionAction> = IntentionHelperUtil.getAiAssistantIntentions(project, editor, file)

        return intentions.map { action ->
            DumbAwareAction.create(action.text) {
                action.invoke(project, editor, file)
            }
        }.toTypedArray()
    }
}
