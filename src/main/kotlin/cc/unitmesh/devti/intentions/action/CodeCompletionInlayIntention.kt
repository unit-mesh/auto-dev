package cc.unitmesh.devti.intentions.action

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.editor.inlay.LLMInlayManager
import cc.unitmesh.devti.intentions.action.base.AbstractChatIntention
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class CodeCompletionInlayIntention : AbstractChatIntention() {
    override fun priority(): Int = 980
    override fun getText(): String = AutoDevBundle.message("intentions.chat.inlay.complete.name")
    override fun getFamilyName(): String = AutoDevBundle.message("intentions.chat.inlay.complete.family.name")
    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return
        val offset = editor.caretModel.offset

        val llmInlayManager = LLMInlayManager.getInstance()
        llmInlayManager
            .editorModified(editor, offset)
    }
}
