package cc.unitmesh.devti.mcp.editor

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.flow.MutableStateFlow
import java.awt.BorderLayout
import java.awt.Dimension
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

    private var editorPanel: JPanel? = null

    data class Tool(val id: String, val name: String, val description: String)

    data class ChatbotConfig(
        var temperature: Double = 0.7,
        var maxTokens: Int = 2000,
        var enabledTools: MutableList<String> = mutableListOf("database", "file", "web")
    )

    private lateinit var toolsContainer: JPanel
    private lateinit var chatbotSelector: JComboBox<String>
    private lateinit var chatInput: JBTextField
    private lateinit var sendButton: JButton
    private lateinit var configButton: JButton
    private val config = ChatbotConfig()
    private val borderColor = JBColor(0xE5E7EB, 0x3C3F41) // Equivalent to Tailwind gray-200
    private val primaryBlue = JBColor(0x3B82F6, 0x589DF6) // Equivalent to Tailwind blue-500
    private val textGray = JBColor(0x6B7280, 0x9DA0A8)    // Equivalent to Tailwind gray-500

    init {
        createUI()
    }

    private fun createUI() {
        val headerPanel = panel {
            row {
                val label = JBLabel("MCP Preview - Tools Panel").apply {
                    fontColor = UIUtil.FontColor.BRIGHTER
                    background = JBColor(0xF5F5F5, 0x2B2D30)
                    font = JBUI.Fonts.label(18.0f).asBold()
                    border = JBUI.Borders.empty(16)
                    isOpaque = true
                }

                cell(label).align(Align.FILL).resizableColumn()

                val subtitle = JBLabel("Available tools and suggested actions").apply {
                    fontColor = UIUtil.FontColor.BRIGHTER
                    font = JBUI.Fonts.label(12.0f)
                    foreground = textGray
                }
                cell(subtitle).align(Align.FILL).resizableColumn()
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

        // Add sample tools
        addTools()

        // Bottom panel for chatbot and input
        val bottomPanel = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
            border = CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, borderColor),
                JBUI.Borders.empty(16)
            )
        }

        // Chatbot selector panel
        val chatbotPanel = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
            border = JBUI.Borders.empty(0, 0, 12, 0)
        }

        val selectorPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
            background = UIUtil.getPanelBackground()
        }

        // Icon placeholder
        val iconPlaceholder = JPanel().apply {
            preferredSize = Dimension(20, 20)
            background = UIUtil.getPanelBackground()
            border = BorderFactory.createLineBorder(primaryBlue, 1)
        }

        val chatbotLabel = JBLabel("Chatbot:").apply {
            font = JBUI.Fonts.label(14.0f)
        }

        chatbotSelector = JComboBox(arrayOf("GPT-4", "Claude", "Llama 3")).apply {
            font = JBUI.Fonts.label(14.0f)
        }

        configButton = JButton("Configure").apply {
            font = JBUI.Fonts.label(14.0f)
            isFocusPainted = false
            addActionListener { showConfigDialog() }
        }

        selectorPanel.add(iconPlaceholder)
        selectorPanel.add(chatbotLabel)
        selectorPanel.add(chatbotSelector)

        val configPanel = JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
            background = UIUtil.getPanelBackground()
            add(configButton)
        }

        chatbotPanel.add(selectorPanel, BorderLayout.WEST)
        chatbotPanel.add(configPanel, BorderLayout.EAST)

        // Chat input panel
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

    private fun addTools() {
        val tools = listOf(
            Tool("database", "Database Tools", "Query and manage databases"),
            Tool("file", "File Operations", "Read, write, and manage files"),
            Tool("web", "Web Tools", "Fetch data from web sources"),
            Tool("ai", "AI Tools", "Image generation and text analysis")
        )

        for (tool in tools) {
            val toolCard = createToolCard(tool)
            toolsContainer.add(toolCard)
        }
    }

    private fun createToolCard(tool: Tool): JPanel {
        val card = JPanel(BorderLayout(0, 8)).apply {
            background = UIUtil.getPanelBackground()
            border = CompoundBorder(
                BorderFactory.createLineBorder(borderColor),
                JBUI.Borders.empty(16)
            )
        }

        // Card header with icon placeholder and title
        val headerPanel = JPanel(BorderLayout(8, 4)).apply {
            background = UIUtil.getPanelBackground()
        }

        // Icon placeholder
        val iconPlaceholder = JPanel().apply {
            preferredSize = Dimension(20, 20)
            background = UIUtil.getPanelBackground()
            border = BorderFactory.createLineBorder(primaryBlue, 1)
        }

        val iconWrapper = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            background = UIUtil.getPanelBackground()
            add(iconPlaceholder)
        }

        val titleLabel = JBLabel(tool.name).apply {
            font = JBUI.Fonts.label(16.0f).asBold()
        }

        val titleWrapper = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
            add(titleLabel, BorderLayout.CENTER)
        }

        val titleRow = JPanel(BorderLayout(8, 0)).apply {
            background = UIUtil.getPanelBackground()
            add(iconWrapper, BorderLayout.WEST)
            add(titleWrapper, BorderLayout.CENTER)
        }

        val descLabel = JBLabel(tool.description).apply {
            font = JBUI.Fonts.label(14.0f)
            foreground = textGray
        }

        headerPanel.add(titleRow, BorderLayout.NORTH)
        headerPanel.add(descLabel, BorderLayout.SOUTH)

        // Card footer with button
        val footerPanel = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
        }

        val detailsButton = JButton("Details").apply {
            font = JBUI.Fonts.label(14.0f)
            isFocusPainted = false
            addActionListener { showToolDetails(tool) }
        }

        footerPanel.add(detailsButton, BorderLayout.CENTER)

        card.add(headerPanel, BorderLayout.CENTER)
        card.add(footerPanel, BorderLayout.SOUTH)

        return card
    }

    private fun showToolDetails(tool: Tool) {
        val dialog = object : DialogWrapper(project) {
            init {
                title = "Tool Details"
                init()
            }

            override fun createCenterPanel(): JComponent {
                return panel {
                    row {
                        label(tool.name).applyToComponent {
                            font = JBUI.Fonts.label(18.0f).asBold()
                        }
                    }
                    row {
                        label(tool.description).applyToComponent {
                            font = JBUI.Fonts.label(14.0f)
                        }
                    }
                    row {
                        label("This tool doesn't require any parameters.").applyToComponent {
                            font = JBUI.Fonts.label(14.0f)
                            foreground = textGray
                        }
                    }.topGap(TopGap.SMALL)
                }.withPreferredSize(400, 200)
            }

            override fun createSouthPanel(): JComponent {
                val panel = JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
                    background = UIUtil.getPanelBackground()
                }

                val executeButton = JButton("Execute").apply {
                    font = JBUI.Fonts.label(14.0f)
                    addActionListener {
                        println("Executing tool: ${tool.id}")
                        close(OK_EXIT_CODE)
                    }
                }

                panel.add(executeButton)
                return panel
            }
        }

        dialog.show()
    }

    private fun showConfigDialog() {
        val dialog = object : DialogWrapper(project) {
            private lateinit var temperatureSlider: JSlider
            private lateinit var tokensSlider: JSlider
            private val toolCheckboxes = mutableMapOf<String, JBCheckBox>()

            init {
                title = "Chatbot Configuration"
                init()
            }

            override fun createCenterPanel(): JComponent {
                return panel {
                    group("Configure ${chatbotSelector.selectedItem}") {
                        row {
                            label("Adjust settings for the selected chatbot").applyToComponent {
                                font = JBUI.Fonts.label(14.0f)
                                foreground = textGray
                            }
                        }

                        row {
                            label("Temperature: ${String.format("%.1f", config.temperature)}")
                        }
                        row {
                            cell(JSlider(0, 10, (config.temperature * 10).toInt()).apply {
                                temperatureSlider = this
                                background = UIUtil.getPanelBackground()
                                addChangeListener {
                                    val value = temperatureSlider.value / 10.0
                                    config.temperature = value
//                                    this.text = "Temperature: ${String.format("%.1f", value)}"
                                }
                            })
                        }
                        row {
                            comment("Lower values produce more focused outputs. Higher values produce more creative outputs.")
                        }

                        row {
                            label("Max Tokens: ${config.maxTokens}")
                        }.topGap(TopGap.MEDIUM)
                        row {
                            cell(JSlider(100, 4000, config.maxTokens).apply {
                                tokensSlider = this
                                background = UIUtil.getPanelBackground()
                                majorTickSpacing = 1000
                                paintTicks = true
                                addChangeListener {
                                    val value = tokensSlider.value
                                    config.maxTokens = value
//                                    component.text = "Max Tokens: $value"
                                }
                            })
                        }
                        row {
                            comment("Maximum number of tokens to generate in the response.")
                        }

                        group("Enabled Tools") {
                            val toolNames = arrayOf("Database Tools", "File Operations", "Web Tools", "AI Tools")
                            val toolIds = arrayOf("database", "file", "web", "ai")

                            for (i in toolNames.indices) {
                                val toolId = toolIds[i]
                                row {
                                    label(toolNames[i])
                                    checkBox("").apply {
                                        component.isSelected = config.enabledTools.contains(toolId)
                                        toolCheckboxes[toolId] = component
                                        component.addActionListener {
                                            if (component.isSelected) {
                                                if (!config.enabledTools.contains(toolId)) {
                                                    config.enabledTools.add(toolId)
                                                }
                                            } else {
                                                config.enabledTools.remove(toolId)
                                            }
                                        }
                                    }
                                }
                            }
                        }.topGap(TopGap.MEDIUM)
                    }
                }.withPreferredSize(400, 500)
            }
        }

        dialog.show()
    }

    private fun sendMessage() {
        val message = chatInput.text.trim()
        if (message.isNotEmpty()) {
            println("Message sent: $message")
            chatInput.text = ""
        }
    }

    fun setMainEditor(editor: Editor) {
        check(mainEditor.value == null)
        mainEditor.value = editor
    }

    fun updateDisplayedContent() {
        // Implementation here
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
    override fun dispose() {}
}
