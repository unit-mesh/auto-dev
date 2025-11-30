package cc.unitmesh.devins.idea.editor

import com.intellij.openapi.editor.ex.EditorEx
import java.util.EventListener

/**
 * Trigger type for input submission.
 */
enum class IdeaInputTrigger {
    Button,
    Key
}

/**
 * Listener interface for input events from IdeaDevInInput.
 * Modeled after AutoDevInputListener from core module.
 */
interface IdeaInputListener : EventListener {
    /**
     * Called when the editor is added to the component.
     */
    fun editorAdded(editor: EditorEx) {}

    /**
     * Called when user submits input (via Enter key or Send button).
     */
    fun onSubmit(text: String, trigger: IdeaInputTrigger) {}

    /**
     * Called when user requests to stop current execution.
     */
    fun onStop() {}

    /**
     * Called when text content changes.
     */
    fun onTextChanged(text: String) {}
}

