package cc.unitmesh.ide.javascript.actions

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import cc.unitmesh.devti.gui.sendToChatPanel
import cc.unitmesh.devti.intentions.action.base.ChatBaseIntention
import cc.unitmesh.devti.llms.LLMProvider
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.ide.javascript.flow.ReactAutoPage
import cc.unitmesh.ide.javascript.util.LanguageApplicableUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class GenComponentAction : ChatBaseIntention() {
    override fun priority(): Int = 1010
    override fun startInWriteAction(): Boolean = false
    override fun getFamilyName(): String = AutoDevBundle.message("frontend.generate")
    override fun getText(): String = AutoDevBundle.message("frontend.component.generate")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return LanguageApplicableUtil.isWebLLMContext(file)
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return
        val selectedText = editor.selectionModel.selectedText ?: return

        val reactAutoPage = ReactAutoPage(project, selectedText, editor)
        sendToChatPanel(project) { contentPanel, _ ->
            val llmProvider = LlmFactory().create(project)
            val prompter = GenComponentFlow(reactAutoPage, contentPanel, llmProvider)

            val task = GenComponentTask(project, prompter, editor)
            ProgressManager.getInstance()
                .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
        }

    }
}

class GenComponentTask(val project: Project, val prompter: GenComponentFlow, val editor: Editor) :
    Task.Backgroundable(project, "Gen Component", true) {
    override fun run(indicator: ProgressIndicator) {

    }
}

class GenComponentFlow(val pages: ReactAutoPage, val contentPanel: ChatCodingPanel, val llmProvider: LLMProvider) {

}
