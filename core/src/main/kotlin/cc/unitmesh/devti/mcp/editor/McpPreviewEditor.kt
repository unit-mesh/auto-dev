package cc.unitmesh.devti.mcp.editor

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.llm2.model.LlmConfig
import cc.unitmesh.devti.llms.custom.CustomLLMProvider
import cc.unitmesh.devti.mcp.client.CustomMcpServerManager
import cc.unitmesh.devti.sketch.ui.patch.readText
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.util.ui.components.BorderLayoutPanel
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import java.beans.PropertyChangeListener
import javax.swing.*
import javax.swing.border.CompoundBorder

open class McpPreviewEditor(
    val project: Project,
    val virtualFile: VirtualFile,
) : UserDataHolder by UserDataHolderBase(), FileEditor {
    val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
    private var mainEditor = MutableStateFlow<Editor?>(null)
    private val mainPanel = JPanel(BorderLayout())

    private val mcpServerManager = CustomMcpServerManager.instance(project)
    private val allTools = mutableMapOf<String, List<Tool>>()
    private var loadingJob: Job? = null
    private val serverLoadingStatus = mutableMapOf<String, Boolean>()
    private val serverPanels = mutableMapOf<String, JPanel>()

    private lateinit var toolsContainer: JPanel
    private lateinit var chatbotSelector: JComboBox<String>
    private lateinit var chatInput: JBTextField
    private lateinit var testButton: ActionButton
    private lateinit var configButton: JButton
    private lateinit var resultPanel: McpResultPanel
    private val config = McpLlmConfig()
    private val borderColor = JBColor(0xE5E7EB, 0x3C3F41) // Equivalent to Tailwind gray-200
    private val textGray = JBColor(0x6B7280, 0x9DA0A8)    // Equivalent to Tailwind gray-500
    private val headerColor = JBColor(0xF3F4F6, 0x2B2D30)  // Light gray for section headers

    init {
        createUI()
        loadTools()
    }

    private fun loadTools() {
        val content = runReadAction { virtualFile.readText() }
        loadingJob?.cancel()
        serverLoadingStatus.clear()
        serverPanels.clear()
        allTools.clear()
        
        SwingUtilities.invokeLater {
            toolsContainer.removeAll()
            toolsContainer.revalidate()
            toolsContainer.repaint()
        }
        
        loadingJob = CoroutineScope(Dispatchers.IO).launch {
            val serverConfigs = mcpServerManager.getServerConfigs(content)
            
            if (serverConfigs.isNullOrEmpty()) {
                SwingUtilities.invokeLater {
                    showNoServersMessage()
                }
                return@launch
            }
            
            SwingUtilities.invokeLater {
                serverConfigs.keys.forEach { serverName ->
                    serverLoadingStatus[serverName] = true
                    createServerSection(serverName)
                }
            }
            
            serverConfigs.forEach { (serverName, serverConfig) ->
                try {
                    val tools = mcpServerManager.collectServerInfo(serverName, serverConfig)
                    allTools[serverName] = tools
                    
                    SwingUtilities.invokeLater {
                        updateServerSection(serverName, tools)
                        serverLoadingStatus[serverName] = false
                    }
                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        showServerError(serverName, e.message ?: "Unknown error")
                        serverLoadingStatus[serverName] = false
                    }
                }
            }
        }
    }
    
    private fun createServerSection(serverName: String) {
        val serverPanel = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, borderColor),
                JBUI.Borders.empty(4, 0)
            )
        }
        
        val headerPanel = JPanel(BorderLayout()).apply {
            background = headerColor
            border = JBUI.Borders.empty(4, 8)
        }
        
        val serverLabel = JBLabel(serverName).apply {
            font = JBUI.Fonts.label(14.0f).asBold()
            foreground = UIUtil.getLabelForeground()
        }
        
        headerPanel.add(serverLabel, BorderLayout.WEST)
        serverPanel.add(headerPanel, BorderLayout.NORTH)
        
        val toolsPanel = JPanel(GridLayout(0, 3, 4, 4)).apply {
            background = UIUtil.getPanelBackground()
            border = JBUI.Borders.empty()
        }
        
        val loadingLabel = JBLabel("Loading tools from $serverName...").apply {
            font = JBUI.Fonts.label(12.0f)
            foreground = textGray
            horizontalAlignment = SwingConstants.LEFT
            icon = AutoDevIcons.LOADING
            iconTextGap = JBUI.scale(8)
        }
        
        toolsPanel.add(loadingLabel)
        serverPanel.add(toolsPanel, BorderLayout.CENTER)
        
        serverPanels[serverName] = toolsPanel
        
        toolsContainer.add(serverPanel)
        toolsContainer.revalidate()
        toolsContainer.repaint()
    }
    
    private fun updateServerSection(serverName: String, tools: List<Tool>) {
        val toolsPanel = serverPanels[serverName] ?: return
        toolsPanel.removeAll()
        
        if (tools.isEmpty()) {
            val noToolsLabel = JBLabel("No tools available for $serverName").apply {
                foreground = textGray
                horizontalAlignment = SwingConstants.LEFT
            }
            toolsPanel.add(noToolsLabel)
        } else {
            tools.forEach { tool ->
                val panel = McpToolDetailPanel(project, serverName, tool)
                toolsPanel.add(panel)
            }
        }
        
        toolsPanel.revalidate()
        toolsPanel.repaint()
    }
    
    private fun showServerError(serverName: String, errorMessage: String) {
        val toolsPanel = serverPanels[serverName] ?: return
        toolsPanel.removeAll()
        
        val errorLabel = JBLabel("Error loading tools: $errorMessage").apply {
            foreground = JBColor.RED
            horizontalAlignment = SwingConstants.LEFT
        }
        
        toolsPanel.add(errorLabel)
        toolsPanel.revalidate()
        toolsPanel.repaint()
    }
    
    private fun showNoServersMessage() {
        toolsContainer.removeAll()
        
        val noServersPanel = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
            border = JBUI.Borders.empty(16)
        }
        
        val noServersLabel = JBLabel("No MCP servers configured. Please check your configuration.").apply {
            foreground = textGray
            horizontalAlignment = SwingConstants.CENTER
        }
        
        noServersPanel.add(noServersLabel, BorderLayout.CENTER)
        toolsContainer.add(noServersPanel)
        toolsContainer.revalidate()
        toolsContainer.repaint()
    }

    fun refreshMcpTool() {
        loadTools()
    }

    private fun createUI() {
        val headerPanel = panel {
            row {
                val label = JBLabel("MCP Preview - Tools Panel").apply {
                    font = JBUI.Fonts.label(14.0f).asBold()
                    isOpaque = true
                }

                cell(label).align(Align.FILL).resizableColumn()
            }
        }.apply {
            border = BorderFactory.createMatteBorder(0, 0, 1, 0, borderColor)
        }

        val toolsWrapper = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
            border = JBUI.Borders.empty(4)
        }

        toolsContainer = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = UIUtil.getPanelBackground()
        }

        val toolsScrollPane = JBScrollPane(toolsContainer).apply {
            border = BorderFactory.createEmptyBorder()
            background = UIUtil.getPanelBackground()
        }

        resultPanel = McpResultPanel().apply {
            background = UIUtil.getPanelBackground()
            isVisible = false
        }
        
        toolsWrapper.add(toolsScrollPane, BorderLayout.CENTER)
        
        val bottomPanel = BorderLayoutPanel().apply {
            background = UIUtil.getPanelBackground()
            border = CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, borderColor),
                JBUI.Borders.empty(4)
            )
        }

        val chatbotPanel = BorderLayoutPanel().apply {
            background = UIUtil.getPanelBackground()
            border = JBUI.Borders.emptyBottom(0)
        }

        val selectorPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            background = UIUtil.getPanelBackground()
        }

        val chatbotLabel = JBLabel("Model")

        val llmConfigs = LlmConfig.load()
        val modelNames = if (llmConfigs.isEmpty()) {
            arrayOf("No LLMs configured")
        } else {
            llmConfigs.map { it.name }.toTypedArray()
        }

        chatbotSelector = com.intellij.openapi.ui.ComboBox(modelNames)

        configButton = JButton("Configure").apply {
            isFocusPainted = false
            addActionListener { showConfigDialog() }
        }

        selectorPanel.add(chatbotLabel)
        selectorPanel.add(chatbotSelector)

        val configPanel = JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
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

        bottomPanel.addToTop(resultPanel)
        bottomPanel.addToCenter(chatbotPanel)
        bottomPanel.addToBottom(inputPanel)

        mainPanel.add(headerPanel, BorderLayout.NORTH)
        mainPanel.add(toolsWrapper, BorderLayout.CENTER)
        mainPanel.add(bottomPanel, BorderLayout.SOUTH)
    }

    private fun showConfigDialog() {
        val dialog = McpLlmConfigDialog(project, config, allTools)

        if (dialog.showAndGet()) {
            config.temperature = dialog.getConfig().temperature
            config.enabledTools = dialog.getConfig().enabledTools
            config.systemPrompt = dialog.getConfig().systemPrompt
        }
    }

    fun sendMessage() {
        val llmConfig = LlmConfig.load().firstOrNull { it.name == chatbotSelector.selectedItem }
            ?: LlmConfig.default()
        val llmProvider = CustomLLMProvider(project, llmConfig)
        val message = chatInput.text.trim()
        val result = StringBuilder()
        val stream: Flow<String> = llmProvider.stream(message, systemPrompt = config.createSystemPrompt())
        
        resultPanel.setText("Loading response...")
        resultPanel.isVisible = true
        mainPanel.revalidate()
        mainPanel.repaint()

        AutoDevCoroutineScope.scope(project).launch {
            stream.cancellable().collect { chunk ->
                result.append(chunk)
                SwingUtilities.invokeLater {
                    resultPanel.setText(result.toString())
                    mainPanel.revalidate()
                    mainPanel.repaint()
                }
            }
        }
    }

    fun setMainEditor(editor: Editor) {
        check(mainEditor.value == null)
        mainEditor.value = editor
    }

    fun scrollToSrcOffset(offset: Int) {
        // Implementation here
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
        loadingJob?.cancel()
    }
}

