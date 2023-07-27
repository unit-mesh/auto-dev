package cc.unitmesh.devti.intentions.task

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.DocumentEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement

class CompletionTaskRequest(
    val project: Project,
    val useTabIndents: Boolean,
    val tabWidth: Int,
    val fileUri: VirtualFile,
    val documentContent: String,
    val offset: Int,
    val documentVersion: Long,
    val element: PsiElement,
    val editor: Editor
) : Disposable {
    companion object {
        fun create(editor: Editor, offset: Int, element: PsiElement, prefix: String?): CompletionTaskRequest? {
            val project = editor.project ?: return null

            val document = editor.document
            val file = PsiDocumentManager.getInstance(project).getPsiFile(document) ?: return null

            val useTabs = editor.settings.isUseTabCharacter(project)
            val tabWidth = editor.settings.getTabSize(project)
            val uri = file.virtualFile
            val documentVersion = if (document is DocumentEx) {
                document.modificationSequence.toLong()
            } else {
                document.modificationStamp
            }

            return CompletionTaskRequest(
                project,
                useTabs,
                tabWidth,
                uri,
                prefix ?: document.text,
                offset,
                documentVersion,
                element,
                editor
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