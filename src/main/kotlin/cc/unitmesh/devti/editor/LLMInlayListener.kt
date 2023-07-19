package cc.unitmesh.devti.editor

import cc.unitmesh.devti.editor.presentation.LLMInlayRenderer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.util.messages.Topic

interface LLMInlayListener {
    fun inlaysUpdated(
        editor: Editor,
        insertedInlays: List<Inlay<LLMInlayRenderer>?>,
    )

    companion object {
        val TOPIC = Topic.create("llm.inlaysUpdate", LLMInlayListener::class.java)
    }
}


