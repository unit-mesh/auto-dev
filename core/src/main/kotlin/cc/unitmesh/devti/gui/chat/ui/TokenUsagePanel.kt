package cc.unitmesh.devti.gui.chat.ui

import cc.unitmesh.devti.gui.chat.ui.TokenUsageViewModel.TokenUsageData
import cc.unitmesh.devti.llms.custom.Usage
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.BorderLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.SwingConstants

/**
 * Panel that displays token usage statistics for the current session
 * Refactored to separate UI concerns from business logic
 */
class TokenUsagePanel(private val project: Project) : BorderLayoutPanel() {
    private val uiComponents = TokenUsageUIComponents()
    private val viewModel = TokenUsageViewModel(project)

    private var currentData: TokenUsageData? = null

    init {
        setupUI()
        setupViewModel()
    }

    private fun setupUI() {
        isOpaque = false
        border = JBUI.Borders.empty(4, 8)

        val mainPanel = createMainPanel()
        addToCenter(mainPanel)

        isVisible = false
    }

    private fun createMainPanel(): JPanel {
        val mainPanel = JPanel(GridBagLayout())
        mainPanel.isOpaque = false

        val gbc = GridBagConstraints()

        // Model label (left)
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.anchor = GridBagConstraints.WEST
        gbc.fill = GridBagConstraints.NONE
        mainPanel.add(uiComponents.createModelLabelPanel(), gbc)

        // Progress bar (center)
        gbc.gridx = 1
        gbc.weightx = 0.9
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = JBUI.insets(0, 8)
        mainPanel.add(uiComponents.createProgressPanel(), gbc)

        // Usage ratio label (right)
        gbc.gridx = 2
        gbc.weightx = 0.1
        gbc.fill = GridBagConstraints.NONE
        gbc.anchor = GridBagConstraints.EAST
        gbc.insets = JBUI.emptyInsets()
        mainPanel.add(uiComponents.createUsageRatioPanel(), gbc)

        return mainPanel
    }

    private fun setupViewModel() {
        viewModel.setOnTokenUsageUpdated { data ->
            updateUI(data)
        }
    }

    private fun updateUI(data: TokenUsageData) {
        currentData = data

        // Update model label
        val modelText = if (!data.model.isNullOrBlank()) {
            "Model: ${data.model}"
        } else {
            ""
        }
        uiComponents.updateModelLabel(modelText)

        // Update progress bar and ratio
        if (data.maxContextWindowTokens > 0) {
            val totalTokens = data.usage.totalTokens
            val usageRatio = (data.usageRatio * 100).toInt().coerceIn(0, 100)

            uiComponents.updateProgressBar(usageRatio, createProgressBarColor(usageRatio))
            uiComponents.updateUsageRatioLabel(
                createUsageRatioText(
                    totalTokens,
                    data.maxContextWindowTokens,
                    usageRatio
                )
            )
            uiComponents.setProgressBarTooltip("Token usage: $usageRatio% of context window")
            uiComponents.setProgressComponentsVisible(true)
        } else {
            uiComponents.setProgressComponentsVisible(false)
        }

        // Update panel visibility
        isVisible = data.isVisible
        revalidate()
        repaint()
    }

    private fun createProgressBarColor(usageRatio: Int): JBColor {
        return when {
            usageRatio >= 90 -> JBColor.RED
            usageRatio >= 75 -> JBColor.ORANGE
            usageRatio >= 50 -> JBColor.YELLOW
            usageRatio >= 25 -> JBColor.GREEN
            else -> UIUtil.getPanelBackground() as JBColor
        }
    }

    private fun createUsageRatioText(totalTokens: Long, maxTokens: Long, usageRatio: Int): String {
        return "${TokenUsageViewModel.formatTokenCount(totalTokens)}/${TokenUsageViewModel.formatTokenCount(maxTokens)} (${usageRatio}%)"
    }

    fun reset() {
        viewModel.reset()
    }

    fun dispose() {
        viewModel.dispose()
    }
}

/**
 * Encapsulates UI component creation and management
 * Separates UI component logic from main panel logic
 */
private class TokenUsageUIComponents {
    val modelLabel = JBLabel("", SwingConstants.LEFT)
    val progressBar = JProgressBar(0, 100)
    val usageRatioLabel = JBLabel("", SwingConstants.CENTER)

    init {
        setupComponents()
    }

    private fun setupComponents() {
        // Setup progress bar
        progressBar.apply {
            isStringPainted = false
            preferredSize = java.awt.Dimension(150, 8)
            minimumSize = java.awt.Dimension(100, 8)
            font = font.deriveFont(Font.PLAIN, 10f)
            isOpaque = false
        }

        // Setup usage ratio label
        usageRatioLabel.apply {
            font = font.deriveFont(Font.PLAIN, 10f)
            foreground = UIUtil.getContextHelpForeground()
            horizontalAlignment = SwingConstants.RIGHT
        }

        // Setup model label
        modelLabel.apply {
            font = font.deriveFont(Font.PLAIN, 11f)
            foreground = UIUtil.getContextHelpForeground()
        }
    }

    fun createModelLabelPanel(): JPanel {
        val leftPanel = JPanel(BorderLayout())
        leftPanel.isOpaque = false
        leftPanel.add(modelLabel, BorderLayout.WEST)
        return leftPanel
    }

    fun createProgressPanel(): JPanel {
        val progressPanel = JPanel(BorderLayout())
        progressPanel.isOpaque = false
        progressPanel.add(progressBar, BorderLayout.CENTER)
        return progressPanel
    }

    fun createUsageRatioPanel(): JPanel {
        val rightPanel = JPanel()
        rightPanel.isOpaque = false
        rightPanel.add(usageRatioLabel)
        return rightPanel
    }

    fun updateModelLabel(text: String) {
        modelLabel.text = text
    }

    fun updateProgressBar(value: Int, color: JBColor) {
        progressBar.value = value
        progressBar.foreground = color
    }

    fun updateUsageRatioLabel(text: String) {
        usageRatioLabel.text = text
    }

    fun setProgressBarTooltip(tooltip: String) {
        progressBar.toolTipText = tooltip
    }

    fun setProgressComponentsVisible(visible: Boolean) {
        progressBar.isVisible = visible
        usageRatioLabel.isVisible = visible
    }
}