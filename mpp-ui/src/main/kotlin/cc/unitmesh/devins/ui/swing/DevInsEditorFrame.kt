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

class DevInsEditorFrame : JFrame("AutoDev Desktop") {

    private val sourceEditor = RSyntaxTextArea(20, 60)
    private val outputArea = JTextArea(10, 60)
    private val statusLabel = JLabel("Ready")
    private val compileButton = JButton("Compile")
    private val clearButton = JButton("Clear")
    private val fileTreePanel = FileTreePanel { file -> openFileInEditor(file) }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentFile: File? = null
    private var mainPanel: JSplitPane? = null
    private var outputPanel: JComponent? = null
    
    init {
        setupUI()
        setupActions()
        setupDefaultContent()
        
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(1400, 900)
        setLocationRelativeTo(null)
    }
    
    private fun setupUI() {
        layout = BorderLayout()
        
        // 工具栏
        val toolbar = createToolbar()
        add(toolbar, BorderLayout.NORTH)
        
        // 初始化主面板
        setupMainPanel(false)
        
        // 状态栏
        val statusPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        statusPanel.add(statusLabel)
        add(statusPanel, BorderLayout.SOUTH)
    }
    
    private fun createToolbar(): JToolBar {
        val toolbar = JToolBar()
        toolbar.isFloatable = false
        
        // 文件操作
        val openProjectButton = JButton("Open Project")
        openProjectButton.addActionListener { openProject() }
        toolbar.add(openProjectButton)

        val openFileButton = JButton("Open File")
        openFileButton.addActionListener { openFile() }
        toolbar.add(openFileButton)
        
        val saveButton = JButton("Save")
        saveButton.addActionListener { saveFile() }
        toolbar.add(saveButton)
        
        toolbar.addSeparator()
        
        // 编译操作（初始隐藏）
        compileButton.isVisible = false
        clearButton.isVisible = false
        toolbar.add(compileButton)
        toolbar.add(clearButton)
        
        return toolbar
    }
    
    private fun createEditorPanel(): JComponent {
        // 代码编辑器
        sourceEditor.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_NONE
        sourceEditor.isCodeFoldingEnabled = true
        sourceEditor.antiAliasingEnabled = true
        sourceEditor.font = Font("JetBrains Mono", Font.PLAIN, 14)
        sourceEditor.tabSize = 2

        val editorScrollPane = RTextScrollPane(sourceEditor)
        editorScrollPane.lineNumbersEnabled = true
        editorScrollPane.isFoldIndicatorEnabled = true
        editorScrollPane.border = TitledBorder("📝 Editor")

        return editorScrollPane
    }
    
    private fun createOutputPanel(): JComponent {
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
              author: "DevIns Team"
              version: "1.0.0"
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

            Edit the variables in the front matter above to see changes!

            Author: ${'$'}author
            Version: ${'$'}version
        """.trimIndent()
    }
    
    private fun compile() {
        compileButton.isEnabled = false
        statusLabel.text = "Compiling..."
        
        scope.launch {
            try {
                val startTime = System.currentTimeMillis()
                
                val result = withContext(Dispatchers.IO) {
                    DevInsCompilerFacade.compile(sourceEditor.text)
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
            fileFilter = FileNameExtensionFilter("All Supported Files",
                "devin", "devins", "kt", "java", "js", "ts", "py", "json", "yaml", "yml", "md", "txt")
            currentDirectory = File(System.getProperty("user.home"))
        }

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            val file = fileChooser.selectedFile
            openFileInEditor(file)
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

    private fun setupMainPanel(showOutput: Boolean) {
        // 移除现有的主面板
        if (mainPanel != null) {
            remove(mainPanel)
        }

        mainPanel = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)

        // 左侧：文件树 + 编辑器
        val leftPanel = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
        leftPanel.leftComponent = fileTreePanel
        leftPanel.rightComponent = createEditorPanel()
        leftPanel.dividerLocation = 250
        leftPanel.resizeWeight = 0.0 // 文件树固定宽度

        if (showOutput) {
            // 三栏布局：文件树 + 编辑器 + 输出
            outputPanel = createOutputPanel()
            mainPanel!!.leftComponent = leftPanel
            mainPanel!!.rightComponent = outputPanel
            mainPanel!!.dividerLocation = 900
            mainPanel!!.resizeWeight = 0.7
        } else {
            // 两栏布局：文件树 + 编辑器
            mainPanel!!.leftComponent = leftPanel
            mainPanel!!.rightComponent = null
            mainPanel!!.dividerLocation = 0
            mainPanel!!.resizeWeight = 1.0
        }

        add(mainPanel, BorderLayout.CENTER)
        revalidate()
        repaint()
    }

    private fun isDevInsFile(file: File): Boolean {
        val extension = file.extension.lowercase()
        return extension in setOf("devin", "devins")
    }

    private fun updateUIForFileType(file: File) {
        val isDevIns = isDevInsFile(file)

        // 更新工具栏按钮可见性
        compileButton.isVisible = isDevIns
        clearButton.isVisible = isDevIns

        // 更新编辑器标题
        val editorScrollPane = sourceEditor.parent.parent as RTextScrollPane
        val fileIcon = when {
            isDevIns -> "📄"
            file.extension.lowercase() in setOf("kt", "java") -> "☕"
            file.extension.lowercase() in setOf("js", "ts") -> "🟨"
            file.extension.lowercase() in setOf("py") -> "🐍"
            file.extension.lowercase() in setOf("json", "yaml", "yml") -> "⚙️"
            file.extension.lowercase() in setOf("md") -> "📝"
            else -> "📄"
        }

        val title = if (isDevIns) "$fileIcon DevIns Source" else "$fileIcon ${file.name}"
        editorScrollPane.border = TitledBorder(title)

        // 更新布局
        setupMainPanel(isDevIns)

        // 重新绘制工具栏
        (compileButton.parent as JToolBar).revalidate()
        (compileButton.parent as JToolBar).repaint()
    }

    private fun openProject() {
        val fileChooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            dialogTitle = "Select Project Directory"
            currentDirectory = File(System.getProperty("user.home"))
        }

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            val selectedDir = fileChooser.selectedFile
            fileTreePanel.loadProject(selectedDir)
            statusLabel.text = "Opened project: ${selectedDir.name}"
        }
    }

    private fun openFileInEditor(file: File) {
        try {
            val content = file.readText()
            sourceEditor.text = content
            currentFile = file
            title = "DevIns Editor - ${file.name}"
            statusLabel.text = "Opened: ${file.name}"

            // 根据文件扩展名设置语法高亮
            when (file.extension.lowercase()) {
                "devin", "devins" -> sourceEditor.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_MARKDOWN
                "md" -> sourceEditor.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_MARKDOWN
                "kt" -> sourceEditor.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_KOTLIN
                "java" -> sourceEditor.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_JAVA
                "js" -> sourceEditor.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT
                "ts" -> sourceEditor.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT
                "py" -> sourceEditor.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_PYTHON
                "json" -> sourceEditor.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_JSON
                "yaml", "yml" -> sourceEditor.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_YAML
                "xml" -> sourceEditor.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_XML
                "html" -> sourceEditor.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_HTML
                "css" -> sourceEditor.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_CSS
                else -> sourceEditor.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_NONE
            }

            // 根据文件类型更新 UI
            updateUIForFileType(file)

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
