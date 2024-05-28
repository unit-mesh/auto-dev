package cc.unitmesh.devti.gui.snippet

import cc.unitmesh.devti.provider.devins.LanguagePromptProcessor
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.psi.PsiManager
import com.intellij.testFramework.utils.editor.getVirtualFile

class AutoDevRunDevInsAction : DumbAwareAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        val editor = e.getData(com.intellij.openapi.actionSystem.PlatformDataKeys.EDITOR) ?: return
        val file = editor.document.getVirtualFile() ?: return
        val project = e.project ?: return

        val language = PsiManager.getInstance(project).findFile(file)?.language?.id ?: return
        e.presentation.isEnabled = language == "DevIn" && LanguagePromptProcessor.instance(language).isNotEmpty()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(com.intellij.openapi.actionSystem.PlatformDataKeys.EDITOR) ?: return
        val project = e.project ?: return

        val document = editor.document
        val text = document.text

        LanguagePromptProcessor.instance("DevIn").firstOrNull()?.compile(project, text)
    }

}
