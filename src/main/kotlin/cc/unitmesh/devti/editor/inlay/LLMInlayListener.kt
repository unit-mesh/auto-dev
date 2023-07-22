package cc.unitmesh.devti.editor.inlay

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.util.messages.Topic

interface LLMInlayListener {
    fun inlaysUpdated(
        editor: Editor,
        insertedInlays: List<Inlay<EditorCustomElementRenderer>?>,
    )

    companion object {
        val TOPIC = Topic.create("llm.inlaysUpdate", LLMInlayListener::class.java)
    }
}


