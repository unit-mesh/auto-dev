package cc.unitmesh.devti.language.task

import cc.unitmesh.devti.language.run.runner.PostFunction
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.DocumentEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement

class ShireCodeCompletionRequest(
    val project: Project,
    val fileUri: VirtualFile,
    val prefixText: String,
    val startOffset: Int,
    val element: PsiElement?,
    val editor: Editor,
    val suffixText: String,
    val isReplacement: Boolean = false,
    val postExecute: PostFunction,
    val isInsertBefore: Boolean,
    val userPrompt: String,
) : Disposable {
    companion object {
        fun create(
            editor: Editor,
            offset: Int,
            element: PsiElement? = null,
            prefix: String? = null,
            suffix: String? = null,
            isReplacement: Boolean = false,
            postExecute: PostFunction,
            isInsertBefore: Boolean = false,
            userPrompt: String,
        ): ShireCodeCompletionRequest? {
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

            val suffixText = suffix ?: document.text.substring(offset)

            return ShireCodeCompletionRequest(
                project,
                file.virtualFile,
                prefix ?: document.text,
                offset,
                element,
                editor,
                suffixText,
                isReplacement,
                postExecute = postExecute,
                isInsertBefore = isInsertBefore,
                userPrompt
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