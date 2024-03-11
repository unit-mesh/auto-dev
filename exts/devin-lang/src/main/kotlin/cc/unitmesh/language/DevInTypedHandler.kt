package cc.unitmesh.language

import cc.unitmesh.language.psi.DevInFile
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class DevInTypedHandler : TypedHandlerDelegate() {

    override fun checkAutoPopup(charTyped: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (file !is DevInFile) {
            return Result.CONTINUE
        }

        return when (charTyped) {
            '@' -> {
                Result.STOP
            }

            '/' -> {
                Result.STOP
            }

            '$' -> {
                Result.STOP
            }

            else -> {
                Result.CONTINUE
            }
        }
    }
}
