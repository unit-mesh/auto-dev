// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cc.unitmesh.terminal

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionHolder
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.terminal.JBTerminalPanel
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.SwingHelper
import com.intellij.util.ui.UIUtil
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory
import org.jetbrains.plugins.terminal.TerminalView
import java.awt.Component
import java.awt.Font
import java.awt.Point
import java.awt.event.*
import java.util.*
import javax.swing.Box
import javax.swing.JTextField
import javax.swing.event.DocumentEvent

private const val OUTLINE_PROPERTY = "JComponent.outline"
private const val ERROR_VALUE = "error"


class ShellCommandSuggestionAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val popupPoint = getPreferredPopupPoint(e)

        val contextComponent = e.getData(PlatformCoreDataKeys.CONTEXT_COMPONENT) ?: return


        showContentRenamePopup(contextComponent, popupPoint) { data ->
            val toolWindow = ToolWindowManager.getInstance(project)
                .getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID) ?: return@showContentRenamePopup
            val content = toolWindow.contentManager.selectedContent ?: return@showContentRenamePopup
            val jbTerminalWidget = TerminalView.getWidgetByContent(content) ?: return@showContentRenamePopup
            jbTerminalWidget.writePlainMessage("echo \"$data\"")
        }
    }

    private fun getPreferredPopupPoint(e: AnActionEvent): RelativePoint? {
        val inputEvent = e.inputEvent
        if (inputEvent is MouseEvent) {
            val comp = inputEvent.getComponent()
            if (comp is AnActionHolder) {
                return RelativePoint(comp.parent, Point(comp.x + JBUI.scale(3), comp.y + comp.height + JBUI.scale(3)))
            }
        }

        return null
    }

    private fun showContentRenamePopup(
        component: Component, popupPoint: RelativePoint?,
        callback: (String) -> Unit
    ) {
        val textField = JTextField().also {
            it.text = AutoDevBundle.message("shell.command.suggestion.action.default.text")
            it.selectAll()
        }

        val label = JBLabel()
        label.font = UIUtil.getLabelFont().deriveFont(Font.BOLD)

        val panel = SwingHelper.newLeftAlignedVerticalPanel(label, Box.createVerticalStrut(JBUI.scale(2)), textField)
        panel.addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent?) {
                IdeFocusManager.findInstance().requestFocus(textField, false)
            }
        })

        val balloon = JBPopupFactory.getInstance().createDialogBalloonBuilder(panel, null)
            .setShowCallout(true)
            .setCloseButtonEnabled(false)
            .setAnimationCycle(0)
            .setHideOnKeyOutside(true)
            .setHideOnClickOutside(true)
            .setRequestFocus(true)
            .setBlockClicksThroughBalloon(true)
            .createBalloon()

        textField.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent?) {
                if (e != null && e.keyCode == KeyEvent.VK_ENTER) {
                    if (textField.text.isEmpty()) {
                        textField.putClientProperty(OUTLINE_PROPERTY, ERROR_VALUE)
                        textField.repaint()
                        return
                    }

                    callback(textField.text)
                    balloon.hide()
                }
            }
        })

        textField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                val outlineValue = textField.getClientProperty(OUTLINE_PROPERTY)
                if (outlineValue == ERROR_VALUE) {
                    textField.putClientProperty(OUTLINE_PROPERTY, null)
                    textField.repaint()
                }
            }
        })

        balloon.show(popupPoint, Balloon.Position.above)
        balloon.addListener(object : JBPopupListener {
            override fun onClosed(event: LightweightWindowEvent) {
                IdeFocusManager.findInstance().requestFocus(component, false)
            }
        })
    }
}