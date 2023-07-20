package cc.unitmesh.devti.intentions.editor

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.models.ConnectorFactory
import cc.unitmesh.devti.models.LLMCoroutineScopeService
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actions.EditorActionUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.jvm.internal.Ref
import kotlin.math.min


class CodeCompletionIntention : AbstractChatIntention() {
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

        val prompt = document.getText(TextRange.create(promptStart, offset))

        val suffixLength = 256
        var suffixEnd = min((offset + suffixLength).toDouble(), document.textLength.toDouble()).toInt()
        while (suffixEnd > offset && !EditorActionUtil.isWordBoundary(editor.document.text, suffixEnd, false, false)) {
            suffixEnd--
        }

//        val element = PsiUtilBase.getElementAtCaret(editor) ?: file
//        val chunksWithPaths = SimilarChunksWithPaths().similarChunksWithPaths(element)
//        val size = chunksWithPaths.chunks?.size ?: 0
//        val similarCode = if (size > 0) {
//            chunksWithPaths.toQuery()
//        } else {
//            ""
//        }
//        prompt = "Code complete for follow code \n$similarCode\n$prompt"
        // TODO: use suffix to improve the completion
//        val suffix = document.getText(TextRange(offset, suffixEnd))

        val task = CodeCompletionTask(editor, prompt, offset)
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))

    }

    companion object {
        val logger = Logger.getInstance(CodeCompletionIntention::class.java)
    }
}
