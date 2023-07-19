package cc.unitmesh.devti.editor

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.util.messages.Topic

interface LLMInlayListener {
    fun inlaysUpdated(
        editor: Editor,
        insertedInlays: List<EditorCustomElementRenderer>,
    )

    companion object {
        val TOPIC = Topic.create("llm.inlaysUpdate", LLMInlayListener::class.java)
    }
}


