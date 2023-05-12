package cc.unitmesh.devti.lsp

import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener

class DevtiLspDocumentListener : DocumentListener {
    override fun documentChanged(event: DocumentEvent) {
        DevtiLSPManager.getInstance().notifyDidChange(event)
    }
}
