package cc.unitmesh.devti.gui.chat.ui

import cc.unitmesh.devti.agent.custom.model.CustomAgentConfig
import cc.unitmesh.devti.gui.AutoDevCoolBorder
import cc.unitmesh.devti.gui.chat.ui.file.FileWorkspaceManager
import com.intellij.ide.IdeTooltip
import com.intellij.ide.IdeTooltipManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon.Position
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.openapi.wm.impl.InternalDecorator
import com.intellij.ui.HintHint
import com.intellij.util.EventDispatcher
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import kotlin.math.max
import kotlin.math.min

class AutoDevInputSection(
    private val project: Project,
    val disposable: Disposable?,
    showAgent: Boolean = true
) : BorderLayoutPanel() {
    val editorListeners = EventDispatcher.create(AutoDevInputListener::class.java)
    private val inputControlsManager = InputControlsManager(project, disposable, editorListeners)
    private val inputSelectorsManager = InputSelectorsManager(project, showAgent)
    private val fileWorkspaceManager = FileWorkspaceManager(project, disposable)

    private val inputPanel = BorderLayoutPanel()

    val focusableComponent: JComponent get() = inputControlsManager.getFocusableComponent()

    fun renderText(): String {
        val inputText = inputControlsManager.renderText()
        val files = fileWorkspaceManager.renderText()
        return if (files.isNotEmpty()) {
            inputText + "\n" + files
        } else {
            inputText
        }
    }

    fun clearText() {
        inputControlsManager.clearText()
    }

    fun setText(text: String) {
        inputControlsManager.setText(text)
    }

    init {
        inputControlsManager.initialize(this)
        val leftPanel = inputSelectorsManager.initialize()
        val headerPanel = fileWorkspaceManager.initialize(inputControlsManager.input)

        setupLayout(leftPanel, headerPanel)
        addListener(object : AutoDevInputListener {
            override fun editorAdded(editor: EditorEx) {
                this@AutoDevInputSection.initEditor()
            }
        })
    }

    private fun setupLayout(leftPanel: JPanel?, headerPanel: JPanel) {
        val layoutPanel = BorderLayoutPanel()
        val horizontalGlue = Box.createHorizontalGlue()
        horizontalGlue.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                IdeFocusManager.getInstance(project).requestFocus(inputControlsManager.input, true)
                inputControlsManager.input.caretModel.moveToOffset(inputControlsManager.input.text.length - 1)
            }
        })
        layoutPanel.setOpaque(false)

        // Add selectors to layout
        if (leftPanel != null) {
            inputControlsManager.input.minimumSize = Dimension(inputControlsManager.input.minimumSize.width, 64)
            layoutPanel.addToLeft(leftPanel)
        } else {
            layoutPanel.addToLeft(inputSelectorsManager.modelSelector)
        }

        layoutPanel.addToCenter(horizontalGlue)
        layoutPanel.addToRight(inputControlsManager.buttonPanel)

        // Setup input panel
        inputPanel.add(inputControlsManager.input, BorderLayout.CENTER)
        inputPanel.addToBottom(layoutPanel)
        inputPanel.addToTop(fileWorkspaceManager.getWorkspacePanel())

        // Add panels to main layout
        this.add(headerPanel, BorderLayout.NORTH)
        this.add(inputPanel, BorderLayout.CENTER)
    }

    // Public API methods delegated to managers

    fun showStopButton() {
        inputControlsManager.showStopButton()
    }

    fun showSendButton() {
        inputControlsManager.showSendButton()
    }

    fun send() {
        inputControlsManager.send()
    }

    fun showTooltip(text: String) {
        showTooltip(inputControlsManager.input, Position.above, text)
    }

    fun showTooltip(component: JComponent, position: Position, text: String) {
        val point = Point(component.x, component.y)
        val hintHint = HintHint(component, point).setAwtTooltip(true).setPreferredPosition(position)
        // Inspired by com.intellij.ide.IdeTooltipManager#showTooltipForEvent
        val tipComponent = IdeTooltipManager.initPane(text, hintHint, null)
        val tooltip = IdeTooltip(component, point, tipComponent)
        IdeTooltipManager.getInstance().show(tooltip, true)
    }

    // Agent management methods
    fun hasSelectedAgent(): Boolean = inputSelectorsManager.hasSelectedAgent()

    fun getSelectedAgent(): CustomAgentConfig = inputSelectorsManager.getSelectedAgent()

    fun selectAgent(config: CustomAgentConfig) = inputSelectorsManager.selectAgent(config)

    fun resetAgent() {
        inputSelectorsManager.resetAgent()
        inputControlsManager.clearText()
        fileWorkspaceManager.clearWorkspace()
    }

    // Input management methods
    fun setContent(text: String) = inputControlsManager.setContent(text)

    fun moveCursorToStart() = inputControlsManager.moveCursorToStart()

    // Editor and UI methods
    fun initEditor() {
        val editorEx = inputControlsManager.input.editor as? EditorEx ?: return
        inputPanel.setBorder(AutoDevCoolBorder(editorEx, this))
        UIUtil.setOpaqueRecursively(this, false)
        this.revalidate()
    }

    override fun getPreferredSize(): Dimension {
        val result = runReadAction { super.getPreferredSize() }
        result.height = max(min(result.height, maxHeight), minimumSize.height)
        return result
    }

    override fun getBackground(): Color? {
        val input = inputControlsManager?.input
        if (input == null) return super.getBackground()

        val editor = input?.editor ?: return super.getBackground()
        return editor.colorsScheme.defaultBackground
    }

    override fun setBackground(bg: Color?) {}

    fun addListener(listener: AutoDevInputListener) {
        editorListeners.addListener(listener)
    }

    private val maxHeight: Int
        get() {
            val decorator = UIUtil.getParentOfType(InternalDecorator::class.java, this)
            val contentManager = decorator?.contentManager ?: return JBUI.scale(200)
            return contentManager.component.height / 2
        }
}