package cc.unitmesh.devti.editor

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.*
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.util.Disposer

class AutoDevEditorListener : EditorFactoryListener {
    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        val project = editor.project ?: return
        if (project.isDisposed) return

        val disposable = Disposer.newDisposable("devtiEditorListener")
        EditorUtil.disposeWithEditor(editor, disposable)
        editor.document.addDocumentListener((DevtiDocumentListener(editor) as DocumentListener), disposable)
    }

    class DevtiDocumentListener(val editor: Editor) : BulkAwareDocumentListener {
        override fun documentChangedNonBulk(event: DocumentEvent) {
            val project = editor.project
            if (project == null || project.isDisposed) return

            val changeOffset = event.offset + event.newLength
            if (editor.caretModel.offset != changeOffset) return

            LOG.warn("documentChangedNonBulk: ${event.document.text}")
            // changeOffset
            LOG.warn("changeOffset: $changeOffset")
        }
    }

    companion object {
        private val LOG = Logger.getInstance(AutoDevEditorListener::class.java)
    }
}
