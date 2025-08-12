package cc.unitmesh.devti.a2a.ui

import cc.unitmesh.devti.a2a.A2AClientConsumer
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import io.a2a.spec.AgentCard
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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

    // Helper methods to safely access AgentCard properties
    // Use reflection to access Java record fields safely
    private fun getAgentName(): String = try {
        getFieldValue(agentCard, "name") as? String ?: "Unknown Agent"
    } catch (e: Exception) {
        "Unknown Agent"
    }

    private fun getAgentDescription(): String = try {
        getFieldValue(agentCard, "description") as? String ?: "No description available"
    } catch (e: Exception) {
        "No description available"
    }

    private fun getAgentVersion(): String = try {
        getFieldValue(agentCard, "version") as? String ?: "1.0.0"
    } catch (e: Exception) {
        "1.0.0"
    }

    private fun getProviderName(): String = try {
        val provider = getFieldValue(agentCard, "provider")
        if (provider != null) {
            getFieldValue(provider, "name") as? String ?: "Unknown"
        } else {
            "Unknown"
        }
    } catch (e: Exception) {
        "Unknown"
    }

    private fun getSkillsCount(): Int = try {
        val skills = getFieldValue(agentCard, "skills") as? List<*>
        skills?.size ?: 0
    } catch (e: Exception) {
        0
    }

    private fun getFieldValue(obj: Any, fieldName: String): Any? = try {
        val field = obj.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.get(obj)
    } catch (e: Exception) {
        null
    }
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

    private class A2AAgentDetailDialog(
        project: Project,
        private val agentCard: AgentCard
    ) : DialogWrapper(project) {
        
        init {
            title = "Agent Details: ${getAgentName()}"
            init()
        }

        private fun getAgentName(): String = try {
            getFieldValue(agentCard, "name") as? String ?: "Unknown Agent"
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
            addDetailRow(panel, "Name", getFieldValue(agentCard, "name") as? String ?: "N/A")
            addDetailRow(panel, "Description", getFieldValue(agentCard, "description") as? String ?: "N/A")
            addDetailRow(panel, "Version", getFieldValue(agentCard, "version") as? String ?: "N/A")
            addDetailRow(panel, "URL", getFieldValue(agentCard, "url") as? String ?: "N/A")
            addDetailRow(panel, "Protocol Version", getFieldValue(agentCard, "protocolVersion") as? String ?: "N/A")

            // Provider Information
            val provider = getFieldValue(agentCard, "provider")
            if (provider != null) {
                addSectionHeader(panel, "Provider")
                addDetailRow(panel, "Name", getFieldValue(provider, "name") as? String ?: "N/A")
                addDetailRow(panel, "Description", getFieldValue(provider, "description") as? String ?: "N/A")
                addDetailRow(panel, "URL", getFieldValue(provider, "url") as? String ?: "N/A")
            }

            // Capabilities
            val capabilities = getFieldValue(agentCard, "capabilities")
            if (capabilities != null) {
                addSectionHeader(panel, "Capabilities")
                addDetailRow(panel, "Supports Streaming", getFieldValue(capabilities, "supportsStreaming")?.toString() ?: "N/A")
                addDetailRow(panel, "Supports Tools", getFieldValue(capabilities, "supportsTools")?.toString() ?: "N/A")
                addDetailRow(panel, "Supports Resources", getFieldValue(capabilities, "supportsResources")?.toString() ?: "N/A")
            }

            // Skills
            val skills = getFieldValue(agentCard, "skills") as? List<*>
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
            val inputModes = getFieldValue(agentCard, "defaultInputModes") as? List<*>
            val outputModes = getFieldValue(agentCard, "defaultOutputModes") as? List<*>
            addDetailRow(panel, "Default Input Modes", inputModes?.joinToString(", ") ?: "N/A")
            addDetailRow(panel, "Default Output Modes", outputModes?.joinToString(", ") ?: "N/A")

            // Additional Information
            addSectionHeader(panel, "Additional Information")
            addDetailRow(panel, "Documentation URL", getFieldValue(agentCard, "documentationUrl") as? String ?: "N/A")
            addDetailRow(panel, "Icon URL", getFieldValue(agentCard, "iconUrl") as? String ?: "N/A")
            addDetailRow(panel, "Preferred Transport", getFieldValue(agentCard, "preferredTransport") as? String ?: "N/A")
            addDetailRow(panel, "Supports Auth Extended Card", getFieldValue(agentCard, "supportsAuthenticatedExtendedCard")?.toString() ?: "N/A")

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

            val skillName = getFieldValue(skill, "name") as? String ?: "Unnamed Skill"
            val skillDesc = getFieldValue(skill, "description") as? String ?: "No description"

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
        project: Project,
        private val agentCard: AgentCard,
        private val a2aClientConsumer: A2AClientConsumer
    ) : DialogWrapper(project) {
        
        private val messageField = JTextField("Hello, how can you help me?")
        private val resultArea = JTextArea()
        
        init {
            title = "Test Agent: ${getAgentName()}"
            init()
        }

        private fun getAgentName(): String = try {
            getFieldValue(agentCard, "name") as? String ?: "Unknown Agent"
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
                lineWrap = true
                wrapStyleWord = true
                isEditable = false
                font = JBUI.Fonts.create("Monospaced", 12)
                text = "Click 'Send' to test the agent..."
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
                // Note: A2AClientConsumer.sendMessage currently doesn't return a response
                // This is a limitation that might need to be addressed
                val agentName = getAgentName()
                val result = a2aClientConsumer.sendMessage(agentName, message)
                resultArea.text = "Message sent successfully to $agentName.\n\n$result"
            } catch (e: Exception) {
                resultArea.text = "Error sending message: ${e.message}"
            }
        }
    }
}
