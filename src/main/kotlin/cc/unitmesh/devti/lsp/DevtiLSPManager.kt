package cc.unitmesh.devti.lsp

import cc.unitmesh.devti.runconfig.AutoCRUDState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.event.DocumentEvent

class DevtiLSPManager {
    fun notifyDidChange(event: DocumentEvent) {
        // todo: implement
    }

    companion object {
        private val logger: Logger = logger<AutoCRUDState>()

        fun getInstance(): DevtiLSPManager {
            return ApplicationManager.getApplication().getService(DevtiLSPManager::class.java)
        }
    }
}
