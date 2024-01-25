package cc.unitmesh.ide.javascript.actions

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.intentions.action.base.ChatBaseIntention
import cc.unitmesh.ide.javascript.flow.ReactFlow
import cc.unitmesh.ide.javascript.util.LanguageApplicableUtil
import com.intellij.openapi.editor.Editor
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

        ReactFlow(project, selectedText, editor)
    }
}
