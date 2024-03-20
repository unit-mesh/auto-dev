package com.intellij.temporary.inlay.codecomplete

import cc.unitmesh.devti.inlay.TypeOverHandler
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.*
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.util.Disposer

@Deprecated("This listener is for automatic code completion triggering. Due to incomplete user interaction logic and massive development workload, it will be replaced by shortcut key triggering which implement by InlayCodeAction")
class AutoDevEditorListener : EditorFactoryListener {
    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        val project = editor.project ?: return
        if (project.isDisposed) return

        val editorDisposable = Disposer.newDisposable("autoDevEditorListener")
        EditorUtil.disposeWithEditor(editor, editorDisposable)

        editor.document.addDocumentListener(AutoDevDocumentListener(editor), editorDisposable)
        editor.caretModel.addCaretListener(AutoDevCaretListener(editor), editorDisposable)
    }

    class AutoDevCaretListener(val editor: Editor) : CaretListener {
        override fun caretPositionChanged(event: CaretEvent) {
            val project = editor.project
            if (project == null || project.isDisposed) return

            val llmInlayManager = LLMInlayManager.getInstance()

            val wasTypeOver = TypeOverHandler.getPendingTypeOverAndReset(editor)
            if (wasTypeOver) {
                llmInlayManager.editorModified(editor)
                return
            }

            if (CommandProcessor.getInstance().currentCommand != null) {
                return
            }

            llmInlayManager.disposeInlays(editor, InlayDisposeContext.CaretChange)
        }
    }

    class AutoDevDocumentListener(val editor: Editor) : BulkAwareDocumentListener {
        override fun documentChangedNonBulk(event: DocumentEvent) {
            val project = editor.project
            if (project == null || project.isDisposed) return

            val commandProcessor = CommandProcessor.getInstance()
            if (commandProcessor.isUndoTransparentActionInProgress) return
            if (commandProcessor.currentCommandName != null) return

            val changeOffset = event.offset + event.newLength
            if (editor.caretModel.offset != changeOffset) return

            val llmInlayManager = LLMInlayManager.getInstance()
            llmInlayManager
                .editorModified(editor, changeOffset)
        }
    }
}