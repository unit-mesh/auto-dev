package cc.unitmesh.devti.intentions.action

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.intentions.action.base.ChatBaseIntention
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.temporary.inlay.codecomplete.LLMInlayManager

class CodeCompletionInlayIntention : ChatBaseIntention() {
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
