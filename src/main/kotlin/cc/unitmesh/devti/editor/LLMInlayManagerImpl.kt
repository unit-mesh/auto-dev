package cc.unitmesh.devti.editor

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project

class LLMInlayManagerImpl : LLMInlayManager {
    override fun isAvailable(editor: Editor): Boolean {
        return false
    }

    override fun applyCompletion(project: Project, editor: Editor) {
        return
    }

    override fun disposeInlays(editor: Editor) {
        return
    }

}
