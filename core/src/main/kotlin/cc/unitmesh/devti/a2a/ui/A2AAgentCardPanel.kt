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

    class A2AAgentDetailDialog(
        project: Project,
        private val agentCard: AgentCard
    ) : DialogWrapper(project) {

        init {
            title = "Agent Details: ${getAgentName()}"
            init()
        }

        private fun getAgentName(): String = try {
            agentCard.name() ?: "Unknown Agent"
        } catch (e: Exception) {
            "Unknown Agent"
        }

        private fun getFieldValue(obj: Any, fieldName: String): Any? = try {
            val field = obj.javaClass.getDeclaredField(fieldName)
            field.isAccessible = true
            field.get(obj)
        } catch (e: Exception) {
            null
        }

        override fun createCenterPanel(): JComponent {
            val panel = JPanel(BorderLayout(0, 10))
            panel.preferredSize = Dimension(600, 500)

            val detailsPanel = createAgentDetailsPanel()
            panel.add(JBScrollPane(detailsPanel), BorderLayout.CENTER)

            return panel
        }

        private fun createAgentDetailsPanel(): JPanel {
            val panel = JPanel()
            panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
            panel.border = JBUI.Borders.empty(16)

            // Basic Information
            addSectionHeader(panel, "Basic Information")
            addDetailRow(panel, "Name", agentCard.name() ?: "N/A")
            addDetailRow(panel, "Description", agentCard.description() ?: "N/A")
            addDetailRow(panel, "Version", agentCard.version() ?: "N/A")
            addDetailRow(panel, "URL", agentCard.url() ?: "N/A")
            addDetailRow(panel, "Protocol Version", agentCard.protocolVersion() ?: "N/A")

            // Provider Information
            val provider = agentCard.provider()
            if (provider != null) {
                addSectionHeader(panel, "Provider")
                try {
                    addDetailRow(panel, "Organization", provider.organization() ?: "N/A")
                    addDetailRow(panel, "URL", provider.url() ?: "N/A")
                } catch (e: Exception) {
                    addDetailRow(panel, "Provider", provider.toString())
                }
            }

            // Capabilities
            val capabilities = agentCard.capabilities()
            if (capabilities != null) {
                addSectionHeader(panel, "Capabilities")
                addDetailRow(panel, "Supports Streaming", capabilities.streaming()?.toString() ?: "N/A")
                addDetailRow(panel, "Push Notifications", capabilities.pushNotifications()?.toString() ?: "N/A")
                addDetailRow(
                    panel,
                    "State Transition History",
                    capabilities.stateTransitionHistory()?.toString() ?: "N/A"
                )
            }

            // Skills
            val skills = agentCard.skills()
            if (!skills.isNullOrEmpty()) {
                addSectionHeader(panel, "Skills (${skills.size})")
                skills.forEach { skill ->
                    if (skill != null) {
                        addSkillRow(panel, skill)
                    }
                }
            }

            // Input/Output Modes
            addSectionHeader(panel, "Input/Output Modes")
            val inputModes = agentCard.defaultInputModes()
            val outputModes = agentCard.defaultOutputModes()
            addDetailRow(panel, "Default Input Modes", inputModes?.joinToString(", ") ?: "N/A")
            addDetailRow(panel, "Default Output Modes", outputModes?.joinToString(", ") ?: "N/A")

            // Additional Information
            addSectionHeader(panel, "Additional Information")
            addDetailRow(panel, "Documentation URL", agentCard.documentationUrl() ?: "N/A")
            addDetailRow(panel, "Icon URL", agentCard.iconUrl() ?: "N/A")
            addDetailRow(panel, "Preferred Transport", agentCard.preferredTransport() ?: "N/A")
            addDetailRow(
                panel,
                "Supports Auth Extended Card",
                agentCard.supportsAuthenticatedExtendedCard()?.toString() ?: "N/A"
            )

            return panel
        }

        private fun addSectionHeader(panel: JPanel, title: String) {
            panel.add(Box.createVerticalStrut(16))
            val headerLabel = JBLabel(title).apply {
                font = JBUI.Fonts.label(14.0f).asBold()
                foreground = JBColor(0x1F2937, 0xF9FAFB)
            }
            panel.add(headerLabel)
            panel.add(Box.createVerticalStrut(8))
        }

        private fun addDetailRow(panel: JPanel, label: String, value: String) {
            val rowPanel = JPanel(BorderLayout()).apply {
                maximumSize = Dimension(Int.MAX_VALUE, 25)
                border = JBUI.Borders.empty(2, 0)
            }

            val labelComponent = JBLabel("$label:").apply {
                font = JBUI.Fonts.label(12.0f).asBold()
                foreground = JBColor(0x374151, 0xD1D5DB)
                preferredSize = Dimension(150, 20)
            }

            val valueComponent = JBLabel(value).apply {
                font = JBUI.Fonts.label(12.0f)
                foreground = JBColor(0x6B7280, 0x9CA3AF)
            }

            rowPanel.add(labelComponent, BorderLayout.WEST)
            rowPanel.add(valueComponent, BorderLayout.CENTER)
            panel.add(rowPanel)
        }

        private fun addSkillRow(panel: JPanel, skill: Any) {
            val skillPanel = JPanel(BorderLayout()).apply {
                maximumSize = Dimension(Int.MAX_VALUE, 40)
                border = JBUI.Borders.empty(4, 16, 4, 0)
                background = JBColor(0xF9FAFB, 0x1F2937)
                isOpaque = true
            }

            // Try to access skill properties using the new API
            val skillName = try {
                when {
                    skill.javaClass.simpleName == "AgentSkill" -> {
                        // Use reflection for AgentSkill record methods
                        val nameMethod = skill.javaClass.getMethod("name")
                        nameMethod.invoke(skill) as? String ?: "Unnamed Skill"
                    }

                    else -> getFieldValue(skill, "name") as? String ?: "Unnamed Skill"
                }
            } catch (e: Exception) {
                "Unnamed Skill"
            }

            val skillDesc = try {
                when {
                    skill.javaClass.simpleName == "AgentSkill" -> {
                        // Use reflection for AgentSkill record methods
                        val descMethod = skill.javaClass.getMethod("description")
                        descMethod.invoke(skill) as? String ?: "No description"
                    }

                    else -> getFieldValue(skill, "description") as? String ?: "No description"
                }
            } catch (e: Exception) {
                "No description"
            }

            val nameLabel = JBLabel("• $skillName").apply {
                font = JBUI.Fonts.label(12.0f).asBold()
                foreground = JBColor(0x1F2937, 0xF9FAFB)
            }

            val descLabel = JBLabel(skillDesc).apply {
                font = JBUI.Fonts.label(11.0f)
                foreground = JBColor(0x6B7280, 0x9CA3AF)
            }

            val skillContent = JPanel()
            skillContent.layout = BoxLayout(skillContent, BoxLayout.Y_AXIS)
            skillContent.add(nameLabel)
            skillContent.add(descLabel)

            skillPanel.add(skillContent, BorderLayout.CENTER)
            panel.add(skillPanel)
        }
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
