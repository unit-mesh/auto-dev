package cc.unitmesh.devti.gui.planner

import cc.unitmesh.devti.inline.AutoDevLineBorder
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.ui.JBColor
import org.jetbrains.annotations.NotNull
import java.awt.event.ActionEvent
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.AbstractAction
import javax.swing.BorderFactory
import javax.swing.KeyStroke
import javax.swing.border.Border

class KeyboardAccessibleActionButton(@NotNull action: AnAction) : ActionButton(
    action,
    action.templatePresentation.clone(),
    "unknown",
    ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
) {
    init {
        isFocusable = true
        inputMap.put(KeyStroke.getKeyStroke("ENTER"), "executeAction")
        actionMap.put("executeAction", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                click()
            }
        })

        val focusListener = AccessibleFocusListener()
        addPropertyChangeListener("border", focusListener)
        addFocusListener(focusListener)
    }

    override fun processKeyEvent(e: KeyEvent?) {
        if (e != null && e.keyCode == KeyEvent.VK_ENTER && e.id == KeyEvent.KEY_PRESSED) {
            click()
        } else {
            super.processKeyEvent(e)
        }
    }

    private inner class AccessibleFocusListener : FocusListener, PropertyChangeListener {
        private var originalBorder: Border? = null
        private var focusedBorder: Border? = null

        override fun focusGained(e: FocusEvent?) {
            val insideBorder = AutoDevLineBorder(JBColor.namedColor("Focus.borderColor", JBColor.BLUE), 1, true, 4)
            focusedBorder = BorderFactory.createCompoundBorder(originalBorder, insideBorder)
            border = focusedBorder
            repaint()
        }

        override fun focusLost(e: FocusEvent?) {
            border = originalBorder
            repaint()
        }

        override fun propertyChange(evt: PropertyChangeEvent?) {
            if (originalBorder == null && evt?.propertyName == "border") {
                val newBorder = evt.newValue as? Border
                if (newBorder != null && newBorder != focusedBorder) {
                    originalBorder = newBorder
                }
            }
        }
    }
}