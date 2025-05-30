package cc.unitmesh.devti.gui.chat.ui

import cc.unitmesh.devti.llm2.TokenUsageEvent
import cc.unitmesh.devti.llm2.TokenUsageListener
import cc.unitmesh.devti.llms.custom.Usage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import javax.swing.Box
import javax.swing.JPanel
import javax.swing.SwingConstants

/**
 * Panel that displays token usage statistics for the current session
 */
class TokenUsagePanel(private val project: Project) : BorderLayoutPanel() {
    
    private val promptTokensLabel = JBLabel("0", SwingConstants.RIGHT)
    private val completionTokensLabel = JBLabel("0", SwingConstants.RIGHT)
    private val totalTokensLabel = JBLabel("0", SwingConstants.RIGHT)
    private val modelLabel = JBLabel("", SwingConstants.LEFT)
    
    private var currentUsage = Usage()
    private var currentModel: String? = null
    
    init {
        setupUI()
        setupTokenUsageListener()
    }
    
    private fun setupUI() {
        isOpaque = false
        border = JBUI.Borders.empty(4, 8)
        
        // Create left panel for model info
        val leftPanel = JPanel(BorderLayout())
        leftPanel.isOpaque = false
        modelLabel.font = modelLabel.font.deriveFont(Font.PLAIN, 11f)
        modelLabel.foreground = UIUtil.getContextHelpForeground()
        leftPanel.add(modelLabel, BorderLayout.WEST)
        
        // Create right panel for token stats
        val rightPanel = JPanel()
        rightPanel.isOpaque = false
        
        // Style the labels
        val labels = listOf(promptTokensLabel, completionTokensLabel, totalTokensLabel)
        labels.forEach { label ->
            label.font = label.font.deriveFont(Font.PLAIN, 11f)
            label.foreground = UIUtil.getContextHelpForeground()
        }
        
        // Add labels with separators
        rightPanel.add(JBLabel("Prompt: ").apply {
            font = font.deriveFont(Font.PLAIN, 11f)
            foreground = UIUtil.getContextHelpForeground()
        })
        rightPanel.add(promptTokensLabel)
        
        rightPanel.add(Box.createHorizontalStrut(8))
        rightPanel.add(JBLabel("Completion: ").apply {
            font = font.deriveFont(Font.PLAIN, 11f)
            foreground = UIUtil.getContextHelpForeground()
        })
        rightPanel.add(completionTokensLabel)
        
        rightPanel.add(Box.createHorizontalStrut(8))
        rightPanel.add(JBLabel("Total: ").apply {
            font = font.deriveFont(Font.PLAIN, 11f)
            foreground = UIUtil.getContextHelpForeground()
        })
        rightPanel.add(totalTokensLabel)
        
        // Add panels to main layout
        addToLeft(leftPanel)
        addToRight(rightPanel)
        
        // Initially hidden
        isVisible = false
    }
    
    private fun setupTokenUsageListener() {
        val messageBus = ApplicationManager.getApplication().messageBus
        messageBus.connect().subscribe(TokenUsageListener.TOPIC, object : TokenUsageListener {
            override fun onTokenUsage(event: TokenUsageEvent) {
                updateTokenUsage(event)
            }
        })
    }
    
    private fun updateTokenUsage(event: TokenUsageEvent) {
        ApplicationManager.getApplication().invokeLater {
            currentUsage = event.usage
            currentModel = event.model
            
            promptTokensLabel.text = formatTokenCount(event.usage.promptTokens ?: 0)
            completionTokensLabel.text = formatTokenCount(event.usage.completionTokens ?: 0)
            totalTokensLabel.text = formatTokenCount(event.usage.totalTokens ?: 0)
            
            if (!event.model.isNullOrBlank()) {
                modelLabel.text = "Model: ${event.model}"
            }
            
            // Show the panel when we have data
            isVisible = true
            revalidate()
            repaint()
        }
    }
    
    private fun formatTokenCount(count: Long): String {
        return when {
            count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
            else -> count.toString()
        }
    }
    
    /**
     * Reset the token usage display
     */
    fun reset() {
        ApplicationManager.getApplication().invokeLater {
            currentUsage = Usage()
            currentModel = null
            promptTokensLabel.text = "0"
            completionTokensLabel.text = "0"
            totalTokensLabel.text = "0"
            modelLabel.text = ""
            isVisible = false
            revalidate()
            repaint()
        }
    }
    
    /**
     * Get current usage for external access
     */
    fun getCurrentUsage(): Usage = currentUsage
    
    /**
     * Get current model for external access
     */
    fun getCurrentModel(): String? = currentModel
}