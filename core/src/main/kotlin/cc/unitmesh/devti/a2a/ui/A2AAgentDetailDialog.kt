package cc.unitmesh.devti.a2a.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import io.a2a.spec.AgentCard
import io.a2a.spec.AgentSkill
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel

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

    /// public record AgentSkill(String id, String name, String description, List<String> tags,
    //                         List<String> examples, List<String> inputModes, List<String> outputModes,
    //                         List<Map<String, List<String>>> security) {
    private fun addSkillRow(panel: JPanel, skill: AgentSkill) {
        val skillPanel = JPanel(BorderLayout()).apply {
            maximumSize = Dimension(Int.MAX_VALUE, 40)
            border = JBUI.Borders.empty(4, 16, 4, 0)
            background = JBColor(0xF9FAFB, 0x1F2937)
            isOpaque = true
        }

        val idLabel = JBLabel("Id: ${skill.id}").apply {
            font = JBUI.Fonts.label(12.0f).asBold()
            foreground = JBColor(0x1F2937, 0xF9FAFB)
        }

        val nameLabel = JBLabel("Name: ${skill.name}").apply {
            font = JBUI.Fonts.label(12.0f).asBold()
            foreground = JBColor(0x1F2937, 0xF9FAFB)
        }

        val descLabel = JBLabel("Desc: ${skill.description}").apply {
            font = JBUI.Fonts.label(11.0f)
            foreground = JBColor(0x6B7280, 0x9CA3AF)
        }

        val skillContent = JPanel()
        skillContent.layout = BoxLayout(skillContent, BoxLayout.Y_AXIS)
        skillContent.add(idLabel)
        skillContent.add(nameLabel)
        skillContent.add(descLabel)

        skillPanel.add(skillContent, BorderLayout.CENTER)
        panel.add(skillPanel)
    }
}