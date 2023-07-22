package cc.unitmesh.devti.actions

import com.intellij.openapi.editor.actionSystem.EditorAction
import com.intellij.openapi.project.DumbAware

class LLMDisposeInlaysAction: EditorAction(LLMDisposeInlaysEditorHandler(null)), DumbAware {
    init {
        setInjectedContext(true)
    }
}

