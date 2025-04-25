package cc.unitmesh.devti.mcp.editor

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevNotifications
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

    private lateinit var toolListPanel: McpToolListPanel
    private lateinit var chatbotSelector: JComboBox<String>
    private lateinit var chatInput: JBTextField
    private lateinit var testButton: ActionButton
    private lateinit var configButton: JButton
    private lateinit var resultPanel: McpChatResultPanel
    private val config = McpChatConfig()
    private val borderColor = JBColor(0xE5E7EB, 0x3C3F41)
    private lateinit var searchField: SearchTextField

    init {
        createUI()
        loadTools()
    }

    private fun loadTools() {
        val content = runReadAction { virtualFile.readText() }
        toolListPanel.loadTools(content)
    }

    fun refreshMcpTool() {
        loadTools()
    }

    private fun createUI() {
        val headerPanel = panel {
            row {
                val label = JBLabel(AutoDevBundle.message("mcp.preview.editor.title")).apply {
                    font = JBUI.Fonts.label(14.0f).asBold()
                    border = JBUI.Borders.emptyLeft(8)
                    isOpaque = true
                }

                cell(label).align(Align.FILL).resizableColumn()
                
                searchField = SearchTextField().apply {
                    textEditor.emptyText.text = AutoDevBundle.message("mcp.preview.editor.search.placeholder")
                    textEditor.document.addDocumentListener(object : DocumentListener {
                        override fun insertUpdate(e: DocumentEvent) = filterTools()
                        override fun removeUpdate(e: DocumentEvent) = filterTools()
                        override fun changedUpdate(e: DocumentEvent) = filterTools()
                    })
                }
                
                cell(searchField).align(Align.FILL).resizableColumn()
            }
        }.apply {
            border = BorderFactory.createMatteBorder(0, 0, 1, 0, borderColor)
        }

        val toolsWrapper = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
            border = JBUI.Borders.empty()
        }

        toolListPanel = McpToolListPanel(project)
        
        val toolsScrollPane = JBScrollPane(toolListPanel).apply {
            border = BorderFactory.createEmptyBorder()
            background = UIUtil.getPanelBackground()
        }

        resultPanel = McpChatResultPanel(project, config).apply {
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
            topComponent = toolsWrapper
            bottomComponent = bottomPanel
            resizeWeight = 0.8
            isContinuousLayout = true
            border = BorderFactory.createEmptyBorder()
            dividerSize = JBUI.scale(5)
        }

        mainPanel.add(headerPanel, BorderLayout.NORTH)
        mainPanel.add(splitPane, BorderLayout.CENTER)
    }

    private fun filterTools() {
        val searchText = searchField.text.trim()
        toolListPanel.filterTools(searchText)
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
    }
}
