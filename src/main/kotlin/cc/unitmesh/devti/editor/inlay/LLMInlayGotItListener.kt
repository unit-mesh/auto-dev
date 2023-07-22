package cc.unitmesh.devti.editor.inlay

import cc.unitmesh.devti.actions.LLMApplyInlaysAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.keymap.KeymapUtil

class LLMInlayGotItListener : LLMInlayListener {
    override fun inlaysUpdated(editor: Editor, insertedInlays: List<Inlay<EditorCustomElementRenderer>?>) {
        if (insertedInlays.isEmpty()) {
            return
        }

        val applyShortcut = KeymapUtil.getShortcutText(LLMApplyInlaysAction.ID)
    }

}