package cc.unitmesh.devins.idea.editor

import cc.unitmesh.llm.NamedModelConfig
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*

/**
 * Swing-based bottom toolbar for the input section.
 * Provides send/stop buttons, model selector, and token info.
 */
class SwingBottomToolbar(
    private val project: Project?,
    private val onSendClick: () -> Unit,
    private val onStopClick: () -> Unit,
    private val onPromptOptimizationClick: () -> Unit
) : JPanel(BorderLayout()) {

    private val modelComboBox = ComboBox<String>()
    private val tokenLabel = JBLabel()
    private val sendButton = JButton("Send", AllIcons.Actions.Execute)
    private val stopButton = JButton("Stop", AllIcons.Actions.Suspend)
    private val optimizeButton = JButton(AllIcons.Actions.Lightning)
    private val settingsButton = JButton(AllIcons.General.Settings)

    private var availableConfigs: List<NamedModelConfig> = emptyList()
    private var onConfigSelect: (NamedModelConfig) -> Unit = {}
    private var onConfigureClick: () -> Unit = {}
    private var isProcessing = false
    private var isEnhancing = false

    init {
        border = JBUI.Borders.empty(4)
        isOpaque = false

        // Left side: Model selector and token info
        val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply {
            isOpaque = false

            modelComboBox.preferredSize = Dimension(150, 28)
            modelComboBox.addActionListener {
                val selectedIndex = modelComboBox.selectedIndex
                if (selectedIndex >= 0 && selectedIndex < availableConfigs.size) {
                    onConfigSelect(availableConfigs[selectedIndex])
                }
            }
            add(modelComboBox)

            tokenLabel.foreground = JBUI.CurrentTheme.Label.disabledForeground()
            add(tokenLabel)
        }
        add(leftPanel, BorderLayout.WEST)

        // Right side: Action buttons
        val rightPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0)).apply {
            isOpaque = false

            settingsButton.apply {
                toolTipText = "MCP Configuration"
                preferredSize = Dimension(32, 28)
                isBorderPainted = false
                isContentAreaFilled = false
                addActionListener { IdeaMcpConfigDialogWrapper.show(project) }
            }
            add(settingsButton)

            optimizeButton.apply {
                toolTipText = "Enhance prompt with AI"
                preferredSize = Dimension(32, 28)
                isBorderPainted = false
                isContentAreaFilled = false
                addActionListener { onPromptOptimizationClick() }
            }
            add(optimizeButton)

            sendButton.apply {
                preferredSize = Dimension(80, 28)
                addActionListener { onSendClick() }
            }
            add(sendButton)

            stopButton.apply {
                preferredSize = Dimension(80, 28)
                isVisible = false
                addActionListener { onStopClick() }
            }
            add(stopButton)
        }
        add(rightPanel, BorderLayout.EAST)
    }

    fun setProcessing(processing: Boolean) {
        isProcessing = processing
        sendButton.isVisible = !processing
        stopButton.isVisible = processing
        optimizeButton.isEnabled = !processing && !isEnhancing
    }

    fun setSendEnabled(enabled: Boolean) {
        sendButton.isEnabled = enabled
    }

    fun setEnhancing(enhancing: Boolean) {
        isEnhancing = enhancing
        optimizeButton.isEnabled = !enhancing && !isProcessing
        optimizeButton.toolTipText = if (enhancing) "Enhancing prompt..." else "Enhance prompt with AI"
    }

    fun setTotalTokens(tokens: Int?) {
        tokenLabel.text = if (tokens != null && tokens > 0) "${tokens}t" else ""
    }

    fun setAvailableConfigs(configs: List<NamedModelConfig>) {
        availableConfigs = configs
        modelComboBox.removeAllItems()
        configs.forEach { modelComboBox.addItem(it.name) }
    }

    fun setCurrentConfigName(name: String?) {
        if (name != null) {
            val index = availableConfigs.indexOfFirst { it.name == name }
            if (index >= 0) {
                modelComboBox.selectedIndex = index
            }
        }
    }

    fun setOnConfigSelect(callback: (NamedModelConfig) -> Unit) {
        onConfigSelect = callback
    }

    fun setOnConfigureClick(callback: () -> Unit) {
        onConfigureClick = callback
    }
}

