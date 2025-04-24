package cc.unitmesh.devti.language.actions.terminal

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.language.actions.DevInsRunFileAction
import cc.unitmesh.devti.devins.ShireActionLocation
import cc.unitmesh.devti.devins.provider.TerminalLocationExecutor
import cc.unitmesh.devti.language.startup.DynamicShireActionService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionHolder
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.SwingHelper
import com.intellij.util.ui.UIUtil
import java.awt.Component
import java.awt.Font
import java.awt.Point
import java.awt.event.*
import javax.swing.Box
import javax.swing.JTextField
import javax.swing.event.DocumentEvent

class AutoDevTerminalAction : DumbAwareAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.EDT
    private val OUTLINE_PROPERTY = "JComponent.outline"
    private val ERROR_VALUE = "error"

    private fun shireActionConfigs(project: Project) =
        DynamicShireActionService.getInstance(project).getActions(ShireActionLocation.TERMINAL_MENU)

    override fun update(e: AnActionEvent) {
        val shireActionConfigs = e.project?.let { shireActionConfigs(it) }
        val firstHole = shireActionConfigs?.firstOrNull()?.hole

        e.presentation.isVisible = shireActionConfigs?.size == 1 && firstHole?.enabled == true
        e.presentation.isEnabled = shireActionConfigs?.size == 1 && firstHole?.enabled == true

        e.presentation.text = firstHole?.description ?: "AutoDev Placeholder (Bug)"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val config = shireActionConfigs(project).firstOrNull() ?: return

        TerminalLocationExecutor.provide(project)?.getComponent(e)?.let { component ->
            showInputBoxPopup(component, getPreferredPopupPoint(e)) { userInput ->
                DevInsRunFileAction.executeFile(project, config, null,
                    variables = mapOf("input" to userInput)
                )
            }
        }
    }

    private fun getPreferredPopupPoint(e: AnActionEvent): RelativePoint? {
        if (e.inputEvent is MouseEvent) {
            val comp = e.inputEvent?.component
            if (comp is AnActionHolder) {
                return RelativePoint(comp.parent, Point(comp.x + JBUI.scale(3), comp.y + comp.height + JBUI.scale(3)))
            }
        }

        return null
    }

    private fun showInputBoxPopup(component: Component, popupPoint: RelativePoint?, callback: (String) -> Unit) {
        val textField = JTextField().also {
            it.text = AutoDevBundle.message("shell.command.suggestion.action.default.text")
            it.selectAll()
        }

        val label = JBLabel().also {
            it.font = UIUtil.getLabelFont().deriveFont(Font.BOLD)
        }

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
