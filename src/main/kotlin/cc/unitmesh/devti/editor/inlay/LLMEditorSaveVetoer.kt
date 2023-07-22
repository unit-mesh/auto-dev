package cc.unitmesh.devti.editor.inlay

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentSynchronizationVetoer
import com.intellij.openapi.util.UserDataHolder

class LLMEditorSaveVetoer : FileDocumentSynchronizationVetoer() {
    override fun maySaveDocument(document: Document, isSaveExplicit: Boolean): Boolean {
        return !LLMInlayManagerImpl.KEY_DOCUMENT_SAVE_VETO.isIn(document as UserDataHolder)
    }
}
