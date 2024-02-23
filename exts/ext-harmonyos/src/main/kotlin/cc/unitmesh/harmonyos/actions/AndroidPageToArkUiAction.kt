package cc.unitmesh.harmonyos.actions

import cc.unitmesh.devti.gui.sendToChatPanel
import cc.unitmesh.devti.intentions.action.base.ChatBaseIntention
import cc.unitmesh.devti.llms.LlmFactory
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiFile

class AndroidPageToArkUiAction : ChatBaseIntention() {
    override fun priority(): Int = 900
    override fun getText(): String = "Android Page to Ark UI"
    override fun getFamilyName(): String = "Android Page to Ark UI"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return System.getProperty("idea.platform.prefix", "idea") == "DevEcoStudio"
                || System.getProperty("idea.platform.prefix", "idea") == "AndroidStudio"
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return
        val selectedText = editor.selectionModel.selectedText ?: return

        val autoUi = AutoArkUi(project, selectedText, editor)

        sendToChatPanel(project) { contentPanel, _ ->
            val llmProvider = LlmFactory().create(project)
//            val context = AutoPageContext.build(reactAutoPage,  language, frameworks)
//            val prompter = AutoPageFlow(context, contentPanel, llmProvider)
//
//            ProgressManager.getInstance()
//                .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
        }
    }
}

class AutoArkUi(project: Project, selectedText: @NlsSafe String, editor: Editor) {

}
