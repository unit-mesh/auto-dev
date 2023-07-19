package cc.unitmesh.devti.editor

import cc.unitmesh.devti.actions.LLMApplyInlaysAction
import cc.unitmesh.devti.editor.presentation.LLMInlayRenderer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.keymap.KeymapUtil

class LLMInlayGotItListener : LLMInlayListener {
    override fun inlaysUpdated(editor: Editor, insertedInlays: List<Inlay<LLMInlayRenderer>?>) {
        if (insertedInlays.isEmpty()) {
            return
        }

        val applyShortcut = KeymapUtil.getShortcutText(LLMApplyInlaysAction.ID)
    }

}