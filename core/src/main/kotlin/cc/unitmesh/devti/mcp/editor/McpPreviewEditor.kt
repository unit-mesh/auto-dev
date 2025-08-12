package cc.unitmesh.devti.mcp.editor

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.a2a.A2aServer
import cc.unitmesh.devti.a2a.ui.A2AAgentListPanel
import cc.unitmesh.devti.llm2.model.LlmConfig
import cc.unitmesh.devti.llms.custom.CustomLLMProvider
import cc.unitmesh.devti.mcp.ui.McpToolListPanel
import cc.unitmesh.devti.mcp.ui.McpChatResultPanel
import cc.unitmesh.devti.mcp.ui.model.McpChatConfig
import cc.unitmesh.devti.mcp.ui.McpChatConfigDialog
import cc.unitmesh.devti.sketch.ui.patch.readText
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.JBColor
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.beans.PropertyChangeListener
import javax.swing.*
import javax.swing.border.CompoundBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

open class McpPreviewEditor(
    val project: Project,
    val virtualFile: VirtualFile,
) : UserDataHolder by UserDataHolderBase(), FileEditor {
    val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
    private var mainEditor = MutableStateFlow<Editor?>(null)
    private val mainPanel = JPanel(BorderLayout())

    private enum class ProtocolType { MCP, A2A }
    private var currentProtocol = ProtocolType.MCP
    private lateinit var protocolToggleButton: JButton

    private lateinit var toolListPanel: McpToolListPanel
    private lateinit var chatbotSelector: JComboBox<String>
    private lateinit var chatInput: JBTextField
    private lateinit var testButton: ActionButton
    private lateinit var configButton: JButton
    private lateinit var resultPanel: McpChatResultPanel
    private val config = McpChatConfig()

    // A2A components
    private lateinit var agentListPanel: A2AAgentListPanel

    private val borderColor = JBColor(0xE5E7EB, 0x3C3F41)
    private lateinit var searchField: SearchTextField

    init {
        createUI()
        loadContent()
    }

    private fun loadContent() {
        when (currentProtocol) {
            ProtocolType.MCP -> loadTools()
            ProtocolType.A2A -> loadAgents()
        }
    }

    private fun loadTools() {
        val content = runReadAction { virtualFile.readText() }
        toolListPanel.loadTools(content)
    }

    private fun loadAgents() {
        val content = runReadAction { virtualFile.readText() }
        agentListPanel.loadAgents(content)
    }

    fun refreshMcpTool() {
        loadTools()
    }

    fun refreshA2AAgents() {
        loadAgents()
    }

    private fun createUI() {
        val headerPanel = panel {
            row {
                val label = JBLabel(getHeaderTitle()).apply {
                    font = JBUI.Fonts.label(14.0f).asBold()
                    border = JBUI.Borders.emptyLeft(8)
                    isOpaque = true
                }

                cell(label).align(Align.FILL).resizableColumn()

                // Protocol toggle button
                protocolToggleButton = JButton(getToggleButtonText()).apply {
                    addActionListener { toggleProtocol() }
                }
                cell(protocolToggleButton)

                searchField = SearchTextField().apply {
                    textEditor.emptyText.text = getSearchPlaceholder()
                    textEditor.document.addDocumentListener(object : DocumentListener {
                        override fun insertUpdate(e: DocumentEvent) = filterContent()
                        override fun removeUpdate(e: DocumentEvent) = filterContent()
                        override fun changedUpdate(e: DocumentEvent) = filterContent()
                    })
                }

                cell(searchField).align(Align.FILL).resizableColumn()
            }
        }.apply {
            border = BorderFactory.createMatteBorder(0, 0, 1, 0, borderColor)
        }

        val contentWrapper = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
            border = JBUI.Borders.empty()
        }

        // Initialize both panels
        toolListPanel = McpToolListPanel(project)
        agentListPanel = A2AAgentListPanel(project)

        val contentScrollPane = JBScrollPane().apply {
            border = BorderFactory.createEmptyBorder()
            background = UIUtil.getPanelBackground()
        }

        // Set initial content based on current protocol
        updateContentPanel(contentScrollPane)

        resultPanel = McpChatResultPanel(project, config).apply {
            background = UIUtil.getPanelBackground()
            isVisible = false
        }

        contentWrapper.add(contentScrollPane, BorderLayout.CENTER)
        
        val bottomPanel = BorderLayoutPanel().apply {
            background = UIUtil.getPanelBackground()
            border = CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, borderColor),
                JBUI.Borders.empty(4)
            )
        }

        val chatbotPanel = BorderLayoutPanel().apply {
            background = UIUtil.getPanelBackground()
            border = CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, borderColor),
                JBUI.Borders.empty()
            )
        }

        val selectorPanel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply {
            background = UIUtil.getPanelBackground()
        }

        val chatbotLabel = JBLabel(AutoDevBundle.message("mcp.preview.editor.model.label"))

        val llmConfigs = LlmConfig.load()
        val modelNames = if (llmConfigs.isEmpty()) {
            arrayOf("Default")
        } else {
            llmConfigs.map { it.name }.toTypedArray()
        }

        chatbotSelector = com.intellij.openapi.ui.ComboBox(modelNames)
        configButton = JButton(AutoDevBundle.message("mcp.preview.editor.configure.button")).apply {
            isFocusPainted = false
            addActionListener {
                showConfigDialog()
            }
        }

        selectorPanel.add(chatbotLabel)
        selectorPanel.add(chatbotSelector)

        val configPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0)).apply {
            background = UIUtil.getPanelBackground()
            add(configButton)
        }

        chatbotPanel.addToLeft(selectorPanel)
        chatbotPanel.addToRight(configPanel)

        val inputPanel = BorderLayoutPanel().apply {
            background = UIUtil.getPanelBackground()
            border = JBUI.Borders.empty()
        }

        chatInput = JBTextField().apply {
            border = CompoundBorder(
                BorderFactory.createLineBorder(borderColor),
                JBUI.Borders.empty(4)
            )
            addActionListener { sendMessage() }
        }

        val sendPresentation = Presentation("Test").apply {
            icon = AutoDevIcons.SEND
            description = "Test Called tools"
        }

        testButton = ActionButton(
            DumbAwareAction.create { sendMessage() },
            sendPresentation, 
            "McpSendAction",
            Dimension(JBUI.scale(30), JBUI.scale(30))
        )
        val sendButtonPanel = JPanel(FlowLayout(FlowLayout.CENTER, 0, 0)).apply {
            background = UIUtil.getPanelBackground()
            isOpaque = false
            add(testButton)
        }

        inputPanel.addToCenter(chatInput)
        inputPanel.addToRight(sendButtonPanel)

        val chatControlsPanel = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
            add(chatbotPanel, BorderLayout.NORTH)
            add(inputPanel, BorderLayout.SOUTH)
        }

        bottomPanel.add(resultPanel, BorderLayout.CENTER)
        bottomPanel.add(chatControlsPanel, BorderLayout.SOUTH)

        val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT).apply {
            topComponent = contentWrapper
            bottomComponent = bottomPanel
            resizeWeight = 0.8
            isContinuousLayout = true
            border = BorderFactory.createEmptyBorder()
            dividerSize = JBUI.scale(5)
        }

        mainPanel.add(headerPanel, BorderLayout.NORTH)
        mainPanel.add(splitPane, BorderLayout.CENTER)
    }

    private fun getHeaderTitle(): String {
        return when (currentProtocol) {
            ProtocolType.MCP -> AutoDevBundle.message("mcp.preview.editor.title")
            ProtocolType.A2A -> "A2A Agent Preview"
        }
    }

    private fun getToggleButtonText(): String {
        return when (currentProtocol) {
            ProtocolType.MCP -> "Switch to A2A"
            ProtocolType.A2A -> "Switch to MCP"
        }
    }

    private fun getSearchPlaceholder(): String {
        return when (currentProtocol) {
            ProtocolType.MCP -> AutoDevBundle.message("mcp.preview.editor.search.placeholder")
            ProtocolType.A2A -> "Search agents..."
        }
    }

    private fun toggleProtocol() {
        currentProtocol = when (currentProtocol) {
            ProtocolType.MCP -> ProtocolType.A2A
            ProtocolType.A2A -> ProtocolType.MCP
        }

        // Update UI
        updateHeaderTitle()
        updateContentPanel()
        updateSearchPlaceholder()
        loadContent()
    }

    private fun updateHeaderTitle() {
        // Find and update the header label
        // This is a simplified approach - in a real implementation you might want to store a reference to the label
        protocolToggleButton.text = getToggleButtonText()
    }

    private fun updateContentPanel(scrollPane: JBScrollPane? = null) {
        val targetScrollPane = scrollPane ?: (mainPanel.components
            .filterIsInstance<JSplitPane>()
            .firstOrNull()?.topComponent as? JPanel)
            ?.components?.filterIsInstance<JBScrollPane>()?.firstOrNull()

        targetScrollPane?.let { sp ->
            when (currentProtocol) {
                ProtocolType.MCP -> sp.setViewportView(toolListPanel)
                ProtocolType.A2A -> sp.setViewportView(agentListPanel)
            }
            sp.revalidate()
            sp.repaint()
        }
    }

    private fun updateSearchPlaceholder() {
        searchField.textEditor.emptyText.text = getSearchPlaceholder()
    }

    private fun filterContent() {
        val searchText = searchField.text.trim()
        when (currentProtocol) {
            ProtocolType.MCP -> toolListPanel.filterTools(searchText)
            ProtocolType.A2A -> agentListPanel.filterAgents(searchText)
        }
    }

    private fun showConfigDialog() {
        val dialog = McpChatConfigDialog(project, config, toolListPanel.getTools())

        if (dialog.showAndGet()) {
            config.temperature = dialog.getConfig().temperature
            config.enabledTools = dialog.getConfig().enabledTools
            config.systemPrompt = dialog.getConfig().systemPrompt
        }
    }

    fun sendMessage() {
        when (currentProtocol) {
            ProtocolType.MCP -> sendMcpMessage()
            ProtocolType.A2A -> sendA2AMessage()
        }
    }

    private fun sendMcpMessage() {
        if (config.enabledTools.isEmpty()) {
            val allTools = toolListPanel.getTools()
            config.enabledTools = allTools.map { it.value }.flatten().toMutableList()
        }

        if (chatInput.text.isEmpty()) {
            AutoDevNotifications.warn(project, AutoDevBundle.message("mcp.preview.editor.empty.message.warning"))
            return
        }

        val llmConfig = LlmConfig.load().firstOrNull { it.name == chatbotSelector.selectedItem }
            ?: LlmConfig.default()
        val llmProvider = CustomLLMProvider(project, llmConfig)
        val message = chatInput.text.trim()

        chatInput.text = ""

        val result = StringBuilder()
        val systemPrompt = config.createSystemPrompt()
        val stream: Flow<String> = llmProvider.stream(message, systemPrompt = systemPrompt)
        
        resultPanel.reset()
        resultPanel.setText(AutoDevBundle.message("mcp.preview.editor.loading.response"))
        resultPanel.isVisible = true
        mainPanel.revalidate()
        mainPanel.repaint()

        AutoDevCoroutineScope.scope(project).launch {
            stream.cancellable().collect { chunk ->
                result.append(chunk)
                SwingUtilities.invokeLater {
                    resultPanel.setText(result.toString())
                }
            }

            resultPanel.parseAndShowTools(result.toString())

            mainPanel.revalidate()
            mainPanel.repaint()
        }
    }

    private fun sendA2AMessage() {
        if (chatInput.text.isEmpty()) {
            AutoDevNotifications.warn(project, "Please enter a message to send.")
            return
        }

        val message = chatInput.text
        val agents = agentListPanel.getAgents()

        if (agents.isEmpty()) {
            AutoDevNotifications.warn(project, "No A2A agents available. Please check your configuration.")
            return
        }

        // For now, send to the first available agent
        // In a real implementation, you might want to let the user select which agent to use
        val firstAgent = agents.values.flatten().firstOrNull()
        if (firstAgent == null) {
            AutoDevNotifications.warn(project, "No A2A agents found.")
            return
        }

        val agentName = firstAgent.name ?: "Unknown Agent"
        resultPanel.isVisible = true
        resultPanel.setText("Sending message to $agentName...")

        AutoDevCoroutineScope.scope(project).launch {
            try {
                val a2aClient = agentListPanel.getA2AClientConsumer()
                val response = a2aClient.sendMessage(agentName, message)

                SwingUtilities.invokeLater {
                    resultPanel.setText("Response from $agentName:\n\n$response")
                    mainPanel.revalidate()
                    mainPanel.repaint()
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    resultPanel.setText("Error sending message: ${e.message}")
                    AutoDevNotifications.error(project, "Failed to send A2A message: ${e.message}")
                }
            }
        }
    }

    fun setMainEditor(editor: Editor) {
        check(mainEditor.value == null)
        mainEditor.value = editor
    }

    override fun getComponent(): JComponent = mainPanel
    override fun getName(): String = "MCP Preview"
    override fun setState(state: FileEditorState) {}
    override fun isModified(): Boolean = false
    override fun isValid(): Boolean = true
    override fun getFile(): VirtualFile = virtualFile
    override fun getPreferredFocusedComponent(): JComponent? = chatInput
    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}
    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}
    override fun dispose() {
        toolListPanel.dispose()
        agentListPanel.dispose()
    }
}
