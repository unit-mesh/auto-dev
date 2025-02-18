package cc.unitmesh.dependencies

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.packageChecker.service.PackageChecker
import com.intellij.packageChecker.service.PackageService
import com.intellij.psi.PsiManager

class AutoDevDependenciesCheck : AnAction("Check dependencies has Issues") {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(PlatformDataKeys.EDITOR) ?: return
        val document = editor.document
        val file = FileDocumentManager.getInstance().getFile(document) ?: return

        val psiFile = PsiManager.getInstance(project).findFile(file) ?: return
        e.presentation.isEnabled = PackageService.getInstance(project).declaredDependencies(psiFile).isNotEmpty()
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(PlatformDataKeys.EDITOR) ?: return
        val document = editor.document
        val file = FileDocumentManager.getInstance().getFile(document) ?: return
        val psiFile = PsiManager.getInstance(project).findFile(file) ?: return

        val dependencies = PackageService.getInstance(project).declaredDependencies(psiFile)
        val checker = PackageChecker.getInstance(project)
        dependencies.forEach {
            checker.packageStatus(it.pkg)
        }
    }
}
