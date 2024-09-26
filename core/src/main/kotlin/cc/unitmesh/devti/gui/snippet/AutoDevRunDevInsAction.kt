package cc.unitmesh.devti.gui.snippet

import cc.unitmesh.devti.provider.devins.LanguagePromptProcessor
import cc.unitmesh.devti.provider.http.HttpClientProvider
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiManager

class AutoDevRunDevInsAction : DumbAwareAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(com.intellij.openapi.actionSystem.PlatformDataKeys.EDITOR) ?: return
        val document = editor.document
        val file = FileDocumentManager.getInstance().getFile(document) ?: return

        val language = try {
            PsiManager.getInstance(project).findFile(file)?.language?.id
        } catch (e: Exception) {
            null
        } ?: return

        e.presentation.isEnabled = language == "HTTP Request" || (language == "DevIn" && hasDevInProcessor(language))
    }

    private fun hasDevInProcessor(language: @NlsSafe String) =
        LanguagePromptProcessor.instance(language).isNotEmpty()

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(com.intellij.openapi.actionSystem.PlatformDataKeys.EDITOR) ?: return
        val project = e.project ?: return

        val document = editor.document
        val text = document.text
        val file = FileDocumentManager.getInstance().getFile(document) ?: return

        val language = PsiManager.getInstance(project).findFile(file)?.language?.id ?: return

        when (language) {
            "HTTP Request" -> {
                // call http request processor
                HttpClientProvider.all().forEach { it.execute(project, file, text) }
            }

            "DevIn" -> {
                LanguagePromptProcessor.instance("DevIn").firstOrNull()?.compile(project, text)
            }
        }
    }
}
