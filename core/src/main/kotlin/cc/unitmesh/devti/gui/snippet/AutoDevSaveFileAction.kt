package cc.unitmesh.devti.gui.snippet

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.refactoring.copy.CopyHandler

class AutoDevSaveFileAction : AnAction(AutoDevBundle.message("autodev.save.action")) {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        val virtualFile = project?.let { e.getData(CommonDataKeys.VIRTUAL_FILE) }
        e.presentation.isEnabled = virtualFile != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        var virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val element: PsiElement? = PsiManager.getInstance(project).findFile(virtualFile)
        if (element == null) return
        CopyHandler.doCopy(arrayOf<PsiElement?>(element.containingFile), null)
    }
}
