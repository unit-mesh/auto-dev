// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cc.unitmesh.devti.inlay

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile

class TypeOverHandler : TypedHandlerDelegate() {
    override fun beforeCharTyped(c: Char, project: Project, editor: Editor, file: PsiFile, fileType: FileType): Result {
        val validTypeOver = c == ')' || c == ']' || c == '}' || c == '"' || c == '\'' || c == '>' || c == ';'
        if (validTypeOver && CommandProcessor.getInstance().currentCommand != null) {
            TYPE_OVER_STAMP[editor] = editor.document.modificationStamp
        } else {
            TYPE_OVER_STAMP[editor] = null
        }

        return Result.CONTINUE
    }

    companion object {
        private val TYPE_OVER_STAMP = Key.create<Long>("copilot.typeOverStamp")
        fun getPendingTypeOverAndReset(editor: Editor): Boolean {
            val stamp = TYPE_OVER_STAMP[editor] ?: return false
            TYPE_OVER_STAMP[editor] = null
            return stamp == editor.document.modificationStamp
        }
    }
}