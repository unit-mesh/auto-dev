package cc.unitmesh.devins.ui.swing

import cc.unitmesh.devins.compiler.DevInsCompilerFacade
import kotlinx.coroutines.*
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rtextarea.RTextScrollPane
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.File
import javax.swing.*
import javax.swing.border.TitledBorder
import javax.swing.filechooser.FileNameExtensionFilter

class DevInsEditorFrame : JFrame("DevIns Editor") {
    
    private val sourceEditor = RSyntaxTextArea(20, 60)
    private val outputArea = JTextArea(10, 60)
    private val variablePanel = VariablePanel()
    private val statusLabel = JLabel("Ready")
    private val compileButton = JButton("Compile")
    private val clearButton = JButton("Clear")
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentFile: File? = null
    
    init {
        setupUI()
        setupActions()
        setupDefaultContent()
        
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(1200, 800)
        setLocationRelativeTo(null)
    }
    
    private fun setupUI() {
        layout = BorderLayout()
        
        // 工具栏
        val toolbar = createToolbar()
        add(toolbar, BorderLayout.NORTH)
        
        // 主面板
        val mainPanel = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
        mainPanel.leftComponent = createLeftPanel()
        mainPanel.rightComponent = createRightPanel()
        mainPanel.dividerLocation = 800
        add(mainPanel, BorderLayout.CENTER)
        
        // 状态栏
        val statusPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        statusPanel.add(statusLabel)
        add(statusPanel, BorderLayout.SOUTH)
    }
    
    private fun createToolbar(): JToolBar {
        val toolbar = JToolBar()
        toolbar.isFloatable = false
        
        // 文件操作
        val openButton = JButton("Open")
        openButton.addActionListener { openFile() }
        toolbar.add(openButton)
        
        val saveButton = JButton("Save")
        saveButton.addActionListener { saveFile() }
        toolbar.add(saveButton)
        
        toolbar.addSeparator()
        
        // 编译操作
        toolbar.add(compileButton)
        toolbar.add(clearButton)
        
        return toolbar
    }
    
    private fun createLeftPanel(): JComponent {
        val leftPanel = JSplitPane(JSplitPane.VERTICAL_SPLIT)
        
        // 代码编辑器
        sourceEditor.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_MARKDOWN
        sourceEditor.isCodeFoldingEnabled = true
        sourceEditor.antiAliasingEnabled = true
        sourceEditor.font = Font("JetBrains Mono", Font.PLAIN, 14)
        sourceEditor.tabSize = 2
        
        val editorScrollPane = RTextScrollPane(sourceEditor)
        editorScrollPane.lineNumbersEnabled = true
        editorScrollPane.isFoldIndicatorEnabled = true
        editorScrollPane.border = TitledBorder("DevIns Source")
        
        leftPanel.topComponent = editorScrollPane
        leftPanel.bottomComponent = variablePanel
        leftPanel.dividerLocation = 400
        
        return leftPanel
    }
    
    private fun createRightPanel(): JComponent {
        outputArea.isEditable = false
        outputArea.font = Font("JetBrains Mono", Font.PLAIN, 12)
        outputArea.background = Color.WHITE
        
        val outputScrollPane = JScrollPane(outputArea)
        outputScrollPane.border = TitledBorder("Output")
        
        return outputScrollPane
    }
    
    private fun setupActions() {
        compileButton.addActionListener {
            compile()
        }
        
        clearButton.addActionListener {
            outputArea.text = ""
            statusLabel.text = "Output cleared"
        }
    }
    
    private fun setupDefaultContent() {
        sourceEditor.text = """
            ---
            name: "DevIns Example"
            variables:
              greeting: "Hello"
              target: "World"
            ---
            
            # DevIns Template Example
            
            ${'$'}greeting, ${'$'}target! Welcome to DevIns.
            
            This is a simple example showing:
            - Variable substitution: ${'$'}greeting and ${'$'}target
            - Front matter configuration
            - Markdown-like syntax
            
            ## Variables in Action
            
            You can use variables like ${'$'}greeting anywhere in your template.
            The compiler will replace them with the actual values.
            
            Try editing the variables in the panel below!
        """.trimIndent()
        
        variablePanel.addVariable("greeting", "Hello")
        variablePanel.addVariable("target", "World")
        variablePanel.addVariable("author", "DevIns Team")
        variablePanel.addVariable("version", "1.0.0")
    }
    
    private fun compile() {
        compileButton.isEnabled = false
        statusLabel.text = "Compiling..."
        
        scope.launch {
            try {
                val startTime = System.currentTimeMillis()
                
                val result = withContext(Dispatchers.IO) {
                    DevInsCompilerFacade.compile(
                        sourceEditor.text,
                        variablePanel.getVariables()
                    )
                }
                
                val executionTime = System.currentTimeMillis() - startTime
                
                withContext(Dispatchers.Main) {
                    if (result.isSuccess()) {
                        outputArea.text = result.output
                        statusLabel.text = "Compilation successful (${executionTime}ms) - " +
                                "Variables: ${result.statistics.variableCount}, " +
                                "Commands: ${result.statistics.commandCount}, " +
                                "Agents: ${result.statistics.agentCount}"
                        outputArea.background = Color.WHITE
                    } else {
                        outputArea.text = "Error: ${result.errorMessage}"
                        statusLabel.text = "Compilation failed"
                        outputArea.background = Color(255, 240, 240)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    outputArea.text = "Exception: ${e.message}"
                    statusLabel.text = "Compilation error"
                    outputArea.background = Color(255, 240, 240)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    compileButton.isEnabled = true
                }
            }
        }
    }
    
    private fun openFile() {
        val fileChooser = JFileChooser().apply {
            fileFilter = FileNameExtensionFilter("DevIns Files", "devin", "devins", "txt")
            currentDirectory = File(System.getProperty("user.home"))
        }
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            val file = fileChooser.selectedFile
            try {
                sourceEditor.text = file.readText()
                currentFile = file
                title = "DevIns Editor - ${file.name}"
                statusLabel.text = "Opened: ${file.name}"
            } catch (e: Exception) {
                JOptionPane.showMessageDialog(
                    this,
                    "Failed to open file: ${e.message}",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }
    }
    
    private fun saveFile() {
        val file = currentFile ?: run {
            val fileChooser = JFileChooser().apply {
                fileFilter = FileNameExtensionFilter("DevIns Files", "devin", "devins")
                currentDirectory = File(System.getProperty("user.home"))
                selectedFile = File("untitled.devin")
            }
            
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                fileChooser.selectedFile
            } else {
                return
            }
        }
        
        try {
            file.writeText(sourceEditor.text)
            currentFile = file
            title = "DevIns Editor - ${file.name}"
            statusLabel.text = "Saved: ${file.name}"
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(
                this,
                "Failed to save file: ${e.message}",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }
}
