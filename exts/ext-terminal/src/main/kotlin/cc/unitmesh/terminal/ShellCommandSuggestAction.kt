// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cc.unitmesh.terminal

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.statusbar.AutoDevStatus
import cc.unitmesh.devti.statusbar.AutoDevStatusService
import cc.unitmesh.devti.template.GENIUS_PRACTISES
import cc.unitmesh.devti.template.TemplateRender
import cc.unitmesh.devti.util.LLMCoroutineScope
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionHolder
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.terminal.JBTerminalWidget
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.SwingHelper
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.jetbrains.plugins.terminal.TerminalProjectOptionsProvider
import java.awt.Component
import java.awt.Font
import java.awt.Point
import java.awt.event.*
import javax.swing.Box
import javax.swing.JTextField
import javax.swing.event.DocumentEvent

private const val OUTLINE_PROPERTY = "JComponent.outline"
private const val ERROR_VALUE = "error"


class ShellCommandSuggestAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val contextComponent = e.getData(PlatformCoreDataKeys.CONTEXT_COMPONENT) ?: return

        showContentRenamePopup(contextComponent, getPreferredPopupPoint(e)) { data ->
            val widget = TerminalUtil.getCurrentTerminalWidget(project) ?: return@showContentRenamePopup
            suggestCommand(widget, data, project)
        }
    }

    private fun suggestCommand(widget: JBTerminalWidget, data: String, project: Project) {
        val templateRender = TemplateRender(GENIUS_PRACTISES)
        val template = templateRender.getTemplate("shell-suggest.vm")

        val options = TerminalProjectOptionsProvider.getInstance(project)

        templateRender.context = ShellSuggestContext(
            data, options.shellPath,
            options.startingDirectory
                ?: project.guessProjectDir()?.path ?: System.getProperty("user.home")
        )
        val promptText = templateRender.renderTemplate(template)

        val llm = LlmFactory.instance.create(project)
        val stringFlow: Flow<String> = llm.stream(promptText, "", false)

        LLMCoroutineScope.scope(project).launch {
            AutoDevStatusService.notifyApplication(AutoDevStatus.InProgress)

            try {
                stringFlow.collect {
                    if (it.contains("\n")) {
                        throw Exception("Shell command suggestion failed")
                    }

                    widget.terminalStarter?.sendString(it, true)
                }
            } finally {
                AutoDevStatusService.notifyApplication(AutoDevStatus.Ready)
            }
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

