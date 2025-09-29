package cc.unitmesh.devti.a2a.ui

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.a2a.A2AClientConsumer
import cc.unitmesh.devti.provider.local.JsonLanguageField
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import io.a2a.spec.AgentCard
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*
import javax.swing.border.CompoundBorder

class A2AAgentCardPanel(
    private val project: Project,
    private val agentCard: AgentCard,
    private val a2aClientConsumer: A2AClientConsumer
) : JPanel(BorderLayout(0, 0)) {
    private fun getAgentName(): String = agentCard.name() ?: "Unknown Agent"
    private fun getAgentDescription(): String = agentCard.description() ?: "No description available"

    private fun getAgentVersion(): String = agentCard.version() ?: "1.0.0"

    private fun getProviderName(): String = agentCard.provider()?.organization() ?: "Unknown"

    private fun getSkillsCount(): Int = agentCard.skills()?.size ?: 0

    private val borderColor = JBColor(0xE5E7EB, 0x3C3F41)
    private val textGray = JBColor(0x6B7280, 0x9DA0A8)
    private val mutedBackground = JBColor(0xF3F4F6, 0x2B2D30)

    private val MAX_AGENT_CARD_HEIGHT = 180
    private val AGENT_CARD_WIDTH = 220

    init {
        buildCardUI()
    }

    private fun buildCardUI() {
        background = UIUtil.getPanelBackground()
        border = CompoundBorder(BorderFactory.createLineBorder(borderColor), JBUI.Borders.empty(4, 8))
        preferredSize = Dimension(AGENT_CARD_WIDTH, MAX_AGENT_CARD_HEIGHT)

        val headerPanel = JPanel(BorderLayout(8, 0)).apply {
            background = UIUtil.getPanelBackground()
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, borderColor),
                JBUI.Borders.empty(2)
            )
        }

        val titleLabel = JBLabel(getAgentName()).apply {
            font = JBUI.Fonts.label(14.0f).asBold()
        }

        val versionLabel = JBLabel("v${getAgentVersion()}").apply {
            font = JBUI.Fonts.label(10.0f)
            foreground = textGray
        }

        headerPanel.add(titleLabel, BorderLayout.CENTER)
        headerPanel.add(versionLabel, BorderLayout.EAST)

        val contentPanel = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
        }

        val descriptionText = getAgentDescription()
        val descLabel = JTextPane().apply {
            text = descriptionText
            font = JBUI.Fonts.label(12.0f)
            foreground = textGray
            isEditable = false
            background = null
            border = null
        }

        contentPanel.add(descLabel, BorderLayout.CENTER)

        // Provider info panel
        val providerPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 2)).apply {
            background = UIUtil.getPanelBackground()
        }

        val providerLabel = JBLabel("Provider: ${getProviderName()}").apply {
            font = JBUI.Fonts.label(11.0f)
            foreground = textGray
            background = mutedBackground
            border = JBUI.Borders.empty(2, 5)
            isOpaque = true
        }

        providerPanel.add(providerLabel)

        // Skills info
        val skillsCount = getSkillsCount()
        if (skillsCount > 0) {
            val skillsLabel = JBLabel("Skills: $skillsCount").apply {
                font = JBUI.Fonts.label(11.0f)
                foreground = textGray
                background = mutedBackground
                border = JBUI.Borders.empty(2, 5)
                isOpaque = true
            }
            providerPanel.add(skillsLabel)
        }

        val footerPanel = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
            border = JBUI.Borders.emptyTop(4)
        }

        val detailsLink = HyperlinkLabel("Details").apply {
            font = JBUI.Fonts.label(12.0f)
            addHyperlinkListener { showAgentDetails() }
        }

        val testLink = HyperlinkLabel("Test").apply {
            font = JBUI.Fonts.label(12.0f)
            addHyperlinkListener { testAgent() }
        }

        val linkWrapperPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0)).apply {
            background = UIUtil.getPanelBackground()
            add(testLink)
            add(detailsLink)
        }

        footerPanel.add(providerPanel, BorderLayout.WEST)
        footerPanel.add(linkWrapperPanel, BorderLayout.EAST)

        add(headerPanel, BorderLayout.NORTH)
        add(contentPanel, BorderLayout.CENTER)
        add(footerPanel, BorderLayout.SOUTH)
    }

    private fun showAgentDetails() {
        val dialog = A2AAgentDetailDialog(project, agentCard)
        dialog.show()
    }

    private fun testAgent() {
        val dialog = A2AAgentTestDialog(project, agentCard, a2aClientConsumer)
        dialog.show()
    }

    private class A2AAgentTestDialog(
        private val project: Project,
        private val agentCard: AgentCard,
        private val a2aClientConsumer: A2AClientConsumer
    ) : DialogWrapper(project) {

        private val messageField = JTextField("Hello, how can you help me?")
        private val resultArea = JsonLanguageField(project, "")

        init {
            title = "Test Agent: ${getAgentName()}"
            init()
        }

        private fun getAgentName(): String = try {
            agentCard.name() ?: "Unknown Agent"
        } catch (e: Exception) {
            "Unknown Agent"
        }

        override fun createCenterPanel(): JComponent {
            val panel = JPanel(BorderLayout(0, 10))
            panel.preferredSize = Dimension(600, 400)

            val topPanel = JPanel(BorderLayout())
            topPanel.add(JLabel("Message:"), BorderLayout.NORTH)
            topPanel.add(messageField, BorderLayout.CENTER)

            val sendButton = JButton("Send").apply {
                addActionListener { sendTestMessage() }
            }
            topPanel.add(sendButton, BorderLayout.EAST)

            val bottomPanel = JPanel(BorderLayout())
            bottomPanel.add(JLabel("Response:"), BorderLayout.NORTH)

            resultArea.apply {
                font = JBUI.Fonts.create("Monospaced", 12)
                setPlaceholder("Click 'Send' to test the agent...")
            }

            bottomPanel.add(JBScrollPane(resultArea), BorderLayout.CENTER)

            panel.add(topPanel, BorderLayout.NORTH)
            panel.add(bottomPanel, BorderLayout.CENTER)

            return panel
        }

        private fun sendTestMessage() {
            val message = messageField.text.trim()
            if (message.isEmpty()) {
                resultArea.text = "Please enter a message to send."
                return
            }

            try {
                resultArea.text = "Sending message..."
                val result = a2aClientConsumer.sendMessage(getAgentName(), message)
                resultArea.text = result
            } catch (e: Exception) {
                resultArea.text = "Error sending message: ${e.message}"
                AutoDevNotifications.error(project, "Failed to send A2A message: ${e.message}")
            }
        }
    }
}
