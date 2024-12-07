package cc.unitmesh.devti.intentions.action.task

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.DocumentEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement

class CodeCompletionRequest(
    val project: Project,
    val useTabIndents: Boolean,
    val tabWidth: Int,
    val fileUri: VirtualFile,
    val prefixText: String,
    val offset: Int,
    val documentVersion: Long,
    val element: PsiElement?,
    val editor: Editor,
    val suffixText: String,
    val isReplacement: Boolean = false,
) : Disposable {
    companion object {
        fun create(
            editor: Editor,
            offset: Int,
            element: PsiElement?,
            prefix: String?,
            suffix: String?,
            isReplacement: Boolean = false
        ): CodeCompletionRequest? {
            val project = editor.project ?: return null
            val document = editor.document
            val file = PsiDocumentManager.getInstance(project).getPsiFile(document) ?: return null

            val useTabs = editor.settings.isUseTabCharacter(project)
            val tabWidth = editor.settings.getTabSize(project)
            val documentVersion = if (document is DocumentEx) {
                document.modificationSequence.toLong()
            } else {
                document.modificationStamp
            }

            val prefixText = prefix ?: document.text.substring(0, offset)
            val suffixText = suffix ?: document.text.substring(offset)

            return CodeCompletionRequest(
                project,
                useTabs,
                tabWidth,
                file.virtualFile,
                prefixText ?: document.text,
                offset,
                documentVersion,
                element,
                editor,
                suffixText,
                isReplacement
            )

        }
    }

    @Volatile
    var isCancelled = false

    fun cancel() {
        if (isCancelled) {
            return
        }
        isCancelled = true
        Disposer.dispose(this)
    }

    override fun dispose() {
        isCancelled = true
    }
}