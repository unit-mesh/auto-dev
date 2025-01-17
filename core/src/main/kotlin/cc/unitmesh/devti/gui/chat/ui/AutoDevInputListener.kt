package cc.unitmesh.devti.gui.chat.ui

import com.intellij.openapi.editor.ex.EditorEx
import java.util.*

enum class AutoDevInputTrigger {
    Button,
    Key
}

interface AutoDevInputListener : EventListener {
    fun editorAdded(editor: EditorEx) {}
    fun onSubmit(component: AutoDevInputSection, trigger: AutoDevInputTrigger) {}
    fun onStop(component: AutoDevInputSection) {}
    fun manualSend(userInput: String) {}
}