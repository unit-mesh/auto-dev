package cc.unitmesh.devti.intentions.action

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.intentions.action.base.ChatBaseIntention
import cc.unitmesh.devti.intentions.action.task.CodeCompletionRequest
import cc.unitmesh.devti.intentions.action.task.RelatedCodeCompletionTask
import cc.unitmesh.devti.intentions.action.task.SimilarCodeCompletionTask
import cc.unitmesh.devti.settings.coder.coderSetting
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actions.EditorActionUtil
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilBase
import kotlin.math.min


class CodeCompletionBaseIntention : ChatBaseIntention() {
    override fun priority(): Int = 981
    override fun getText(): String = AutoDevBundle.message("intentions.chat.code.complete.name")
    override fun getFamilyName(): String = AutoDevBundle.message("intentions.chat.code.complete.family.name")
    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return

        val document = editor.document
        val offset = editor.caretModel.offset
        val promptLength = 512

        var promptStart = (offset - promptLength).coerceAtLeast(0)
        val isOutBoundary = !EditorActionUtil.isWordBoundary(editor.document.text, promptStart, false, false)
        while (promptStart < offset && isOutBoundary) {
            promptStart++
        }
        if (promptStart == offset) {
            promptStart = (offset - promptLength).coerceAtLeast(0)
        }

        val prefix = document.getText(TextRange.create(promptStart, offset))

        val suffixLength = 256
        var suffixEnd = min((offset + suffixLength).toDouble(), document.textLength.toDouble()).toInt()
        while (suffixEnd > offset && !EditorActionUtil.isWordBoundary(editor.document.text, suffixEnd, false, false)) {
            suffixEnd--
        }

        val element = PsiUtilBase.getElementAtCaret(editor) ?: file
        val suffix = document.getText(TextRange(offset, suffixEnd))

        val request = CodeCompletionRequest.create(editor, offset, element, prefix, suffix) ?: return

        if (project.coderSetting.state.inEditorCompletion) {
            val task = RelatedCodeCompletionTask(request)
            ProgressManager.getInstance()
                .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
        } else {
            val task = SimilarCodeCompletionTask(request)
            ProgressManager.getInstance()
                .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
        }
    }
}
