package com.intellij.ml.llm.core.chat.ui.editor

import com.intellij.ide.KeyboardAwareFocusOwner
import java.awt.Dimension
import java.awt.event.KeyEvent
import javax.swing.JTextField

class AIInplacePrompt : JTextField(), KeyboardAwareFocusOwner {
    override fun skipKeyEventDispatcher(event: KeyEvent): Boolean {
        return true
    }

    override fun getPreferredSize(): Dimension {
        val size = super.getPreferredSize()
        val it = parent
        if (it != null) {
            size.width = it.getMaximumSize().width
        }
        return size
    }
}
