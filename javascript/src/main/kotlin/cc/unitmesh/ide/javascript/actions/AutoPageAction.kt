package cc.unitmesh.ide.javascript.actions

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.gui.sendToChatPanel
import cc.unitmesh.devti.intentions.action.base.ChatBaseIntention
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.ide.javascript.JsDependenciesSnapshot
import cc.unitmesh.ide.javascript.flow.model.AutoPageContext
import cc.unitmesh.ide.javascript.flow.AutoPageFlow
import cc.unitmesh.ide.javascript.flow.AutoPageTask
import cc.unitmesh.ide.javascript.flow.ReactAutoPage
import cc.unitmesh.ide.javascript.util.LanguageApplicableUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class AutoPageAction : ChatBaseIntention() {
    override fun priority(): Int = 1010
    override fun startInWriteAction(): Boolean = false
    override fun getFamilyName(): String = AutoDevBundle.message("autopage.generate")
    override fun getText(): String = AutoDevBundle.message("autopage.generate.name")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return LanguageApplicableUtil.isWebLLMContext(file)
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return
        val selectedText = editor.selectionModel.selectedText ?: return

        val snapshot = JsDependenciesSnapshot.create(project, file)
        val language = snapshot.language()
        val frameworks = snapshot.mostPopularFrameworks()

        val reactAutoPage = ReactAutoPage(project, selectedText, editor)

        sendToChatPanel(project) { contentPanel, _ ->
            val llmProvider = LlmFactory().create(project)
            val context = AutoPageContext.build(reactAutoPage,  language, frameworks)
            val prompter = AutoPageFlow(context, contentPanel, llmProvider)

            val task = AutoPageTask(project, prompter, editor, reactAutoPage)
            ProgressManager.getInstance()
                .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
        }

    }
}

