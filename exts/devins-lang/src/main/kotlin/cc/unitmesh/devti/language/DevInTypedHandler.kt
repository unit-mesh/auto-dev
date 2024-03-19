// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cc.unitmesh.devti.language

import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.language.psi.DevInTypes
import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType

class DevInTypedHandler : TypedHandlerDelegate() {
    override fun checkAutoPopup(charTyped: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (file !is DevInFile) {
            return Result.CONTINUE
        }

        return when (charTyped) {
            '`' -> {
                val offset = editor.caretModel.primaryCaret.offset
                if (offset == 0) return Result.CONTINUE

                val element = file.findElementAt(offset - 1)
                if (element?.elementType == DevInTypes.CODE_CONTENT || element?.elementType == DevInTypes.CODE_BLOCK_END) {
                    return Result.CONTINUE
                }

                PsiDocumentManager.getInstance(project).commitDocument(editor.document)
                AutoPopupController.getInstance(project).autoPopupMemberLookup(editor, null)
                return Result.STOP
            }

            '@', '/', '$', ':' -> {
                PsiDocumentManager.getInstance(project).commitDocument(editor.document)
                AutoPopupController.getInstance(project).autoPopupMemberLookup(editor, null)
                Result.STOP
            }

            else -> {
                Result.CONTINUE
            }
        }
    }
}
