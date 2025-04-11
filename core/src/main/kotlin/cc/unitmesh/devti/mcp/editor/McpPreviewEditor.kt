package cc.unitmesh.devti.mcp.editor

import cc.unitmesh.devti.llm2.model.LlmConfig
import cc.unitmesh.devti.mcp.client.CustomMcpServerManager
import cc.unitmesh.devti.sketch.ui.patch.readText
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
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridLayout
import java.beans.PropertyChangeListener
import javax.swing.*
import javax.swing.border.CompoundBorder

/**
 * Display shire file render prompt and have a sample file as view
 */
open class McpPreviewEditor(
    val project: Project,
    val virtualFile: VirtualFile,
) : UserDataHolder by UserDataHolderBase(), FileEditor {
    val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
    private var mainEditor = MutableStateFlow<Editor?>(null)
    private val mainPanel = JPanel(BorderLayout())
    private val visualPanel: JBScrollPane = JBScrollPane(
        mainPanel,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    )

    private val mcpServerManager = CustomMcpServerManager.instance(project)
    private val allTools = mutableMapOf<String, List<Tool>>()
    private var loadingJob: Job? = null

    private lateinit var toolsContainer: JPanel
    private lateinit var chatbotSelector: JComboBox<String>
    private lateinit var chatInput: JBTextField
    private lateinit var sendButton: JButton
    private lateinit var configButton: JButton
    private val config = McpLlmConfig()
    private val borderColor = JBColor(0xE5E7EB, 0x3C3F41) // Equivalent to Tailwind gray-200
    private val primaryBlue = JBColor(0x3B82F6, 0x589DF6) // Equivalent to Tailwind blue-500
    private val textGray = JBColor(0x6B7280, 0x9DA0A8)    // Equivalent to Tailwind gray-500
    
    init {
        createUI()
        loadTools()
    }

    private fun loadTools() {
        val content = runReadAction { virtualFile.readText() }
        loadingJob?.cancel()
        loadingJob = CoroutineScope(Dispatchers.IO).launch {
            val serverConfigs = mcpServerManager.getServerConfigs(content)
            serverConfigs?.forEach { (serverName, serverConfig) ->
                try {
                    val tools = mcpServerManager.collectServerInfo(serverName, serverConfig)
                    if (tools.isNotEmpty()) {
                        allTools[serverName] = tools
                        SwingUtilities.invokeLater {
                            updateToolsContainer()
                        }
                    }
                } catch (e: Exception) {
                    // Handle exception for this server but continue with others
                    println("Error loading tools from server $serverName: ${e.message}")
                }
            }
        }
    }


    fun refreshMcpTool() {
        loadTools()
    }

    private fun createUI() {
        val headerPanel = panel {
            row {
                val label = JBLabel("MCP Preview - Tools Panel").apply {
                    font = JBUI.Fonts.label(18.0f).asBold()
                    isOpaque = true
                }

                cell(label).align(Align.FILL).resizableColumn()
            }
        }.apply {
            border = BorderFactory.createMatteBorder(0, 0, 1, 0, borderColor)
        }

        // Tools container panel
        val toolsWrapper = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
            border = JBUI.Borders.empty(16)
        }

        toolsContainer = JPanel(GridLayout(0, 2, 16, 16)).apply {
            background = UIUtil.getPanelBackground()
        }

        val toolsScrollPane = JBScrollPane(toolsContainer).apply {
            border = BorderFactory.createEmptyBorder()
            verticalScrollBar.unitIncrement = 16
            background = UIUtil.getPanelBackground()
        }

        toolsWrapper.add(toolsScrollPane, BorderLayout.CENTER)
        val bottomPanel = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
            border = CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, borderColor),
                JBUI.Borders.empty(16)
            )
        }

        val chatbotPanel = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
            border = JBUI.Borders.emptyBottom(12)
        }

        val selectorPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
            background = UIUtil.getPanelBackground()
        }

        val chatbotLabel = JBLabel("Chatbot:").apply {
            font = JBUI.Fonts.label(14.0f)
        }

        val llmConfigs = LlmConfig.load()
        val modelNames = if (llmConfigs.isEmpty()) {
            arrayOf("No LLMs configured")
        } else {
            llmConfigs.map { it.name }.toTypedArray()
        }

        chatbotSelector = com.intellij.openapi.ui.ComboBox(modelNames).apply {
            font = JBUI.Fonts.label(14.0f)
        }

        configButton = JButton("Configure").apply {
            font = JBUI.Fonts.label(14.0f)
            isFocusPainted = false
            addActionListener { showConfigDialog() }
        }

        selectorPanel.add(chatbotLabel)
        selectorPanel.add(chatbotSelector)

        val configPanel = JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
            background = UIUtil.getPanelBackground()
            add(configButton)
        }

        chatbotPanel.add(selectorPanel, BorderLayout.WEST)
        chatbotPanel.add(configPanel, BorderLayout.EAST)

        val inputPanel = JPanel(BorderLayout(8, 0)).apply {
            background = UIUtil.getPanelBackground()
        }

        chatInput = JBTextField().apply {
            font = JBUI.Fonts.label(14.0f)
            border = CompoundBorder(
                BorderFactory.createLineBorder(borderColor),
                JBUI.Borders.empty(8)
            )
            addActionListener { sendMessage() }
        }

        sendButton = JButton("Send").apply {
            font = JBUI.Fonts.label(14.0f)
            isFocusPainted = false
            addActionListener { sendMessage() }
        }

        inputPanel.add(chatInput, BorderLayout.CENTER)
        inputPanel.add(sendButton, BorderLayout.EAST)

        bottomPanel.add(chatbotPanel, BorderLayout.NORTH)
        bottomPanel.add(inputPanel, BorderLayout.SOUTH)

        // Add all panels to main panel
        mainPanel.add(headerPanel, BorderLayout.NORTH)
        mainPanel.add(toolsWrapper, BorderLayout.CENTER)
        mainPanel.add(bottomPanel, BorderLayout.SOUTH)
    }

    private fun updateToolsContainer() {
        toolsContainer.removeAll()
        
        if (allTools.isEmpty()) {
            val noToolsLabel = JBLabel("No tools available. Please check MCP server configuration.").apply {
                font = JBUI.Fonts.label(14.0f)
                foreground = textGray
            }
            toolsContainer.add(noToolsLabel)
        } else {
            allTools.forEach { (serverName, tools) ->
                tools.forEach { tool ->
                    val panel = McpToolDetailPanel(project, serverName, tool)
                    toolsContainer.add(panel)
                }
            }
        }
        
        toolsContainer.revalidate()
        toolsContainer.repaint()
    }

    private fun showConfigDialog() {
        val dialog = McpLlmConfigDialog(
            project,
            config,
            chatbotSelector.selectedItem.toString(),
            allTools
        )
        
        if (dialog.showAndGet()) {
            config.systemPrompt = dialog.getPromptText()
        }
    }

    private fun sendMessage() {
        val message = chatInput.text.trim()
        if (message.isNotEmpty()) {
            chatInput.text = ""
        }
    }

    fun setMainEditor(editor: Editor) {
        check(mainEditor.value == null)
        mainEditor.value = editor
    }

    fun scrollToSrcOffset(offset: Int) {
        // Implementation here
    }

    override fun getComponent(): JComponent = visualPanel
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
