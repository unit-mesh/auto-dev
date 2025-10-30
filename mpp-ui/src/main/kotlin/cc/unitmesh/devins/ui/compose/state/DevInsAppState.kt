package cc.unitmesh.devins.ui.compose.state

import androidx.compose.runtime.*
import cc.unitmesh.devins.compiler.DevInsCompilerFacade
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Stable
class DevInsAppState(
    private val scope: CoroutineScope
) {
    var editorContent by mutableStateOf(getDefaultContent())
        private set
    
    var output by mutableStateOf("")
        private set
    
    var isOutputError by mutableStateOf(false)
        private set
    
    var statusMessage by mutableStateOf("Ready")
        private set
    
    var currentFile by mutableStateOf<File?>(null)
        private set
    
    var projectRoot by mutableStateOf<File?>(null)
        private set
    
    var isCompiling by mutableStateOf(false)
        private set
    
    val canCompile: Boolean
        get() = editorContent.isNotBlank() && !isCompiling
    
    val showFileTree: Boolean
        get() = projectRoot != null
    
    val showOutput: Boolean
        get() = output.isNotBlank()
    
    fun updateEditorContent(content: String) {
        editorContent = content
    }
    
    fun openFile() {
        val fileChooser = JFileChooser().apply {
            fileFilter = FileNameExtensionFilter(
                "All Supported Files",
                "devin", "devins", "kt", "java", "js", "ts", "py", "json", "yaml", "yml", "md", "txt"
            )
            currentDirectory = currentFile?.parentFile ?: File(System.getProperty("user.home"))
        }
        
        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            openFileInEditor(fileChooser.selectedFile)
        }
    }
    
    fun openProject() {
        val fileChooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            dialogTitle = "Select Project Directory"
            currentDirectory = projectRoot ?: File(System.getProperty("user.home"))
        }
        
        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            projectRoot = fileChooser.selectedFile
            statusMessage = "Opened project: ${fileChooser.selectedFile.name}"
        }
    }
    
    fun openFileInEditor(file: File) {
        try {
            currentFile = file
            editorContent = file.readText()
            statusMessage = "Opened: ${file.name}"
        } catch (e: Exception) {
            statusMessage = "Error opening file: ${e.message}"
        }
    }
    
    fun saveCurrentFile() {
        currentFile?.let { file ->
            try {
                file.writeText(editorContent)
                statusMessage = "Saved: ${file.name}"
            } catch (e: Exception) {
                statusMessage = "Error saving file: ${e.message}"
            }
        } ?: run {
            statusMessage = "No file to save"
        }
    }
    
    fun compile() {
        if (!canCompile) return
        
        isCompiling = true
        statusMessage = "Compiling..."
        
        scope.launch {
            try {
                val startTime = System.currentTimeMillis()
                
                val result = withContext(Dispatchers.IO) {
                    DevInsCompilerFacade.compile(editorContent)
                }
                
                val executionTime = System.currentTimeMillis() - startTime
                
                withContext(Dispatchers.Main) {
                    if (result.isSuccess()) {
                        output = result.output
                        isOutputError = false
                        statusMessage = "Compilation successful (${executionTime}ms) - " +
                                "Variables: ${result.statistics.variableCount}, " +
                                "Commands: ${result.statistics.commandCount}, " +
                                "Agents: ${result.statistics.agentCount}"
                    } else {
                        output = "Error: ${result.errorMessage}"
                        isOutputError = true
                        statusMessage = "Compilation failed"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    output = "Exception: ${e.message}"
                    isOutputError = true
                    statusMessage = "Compilation error"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isCompiling = false
                }
            }
        }
    }
    
    fun clearOutput() {
        output = ""
        isOutputError = false
        statusMessage = "Output cleared"
    }
    
    private fun getDefaultContent(): String = """
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

@Composable
fun rememberDevInsAppState(
    scope: CoroutineScope = rememberCoroutineScope()
): DevInsAppState {
    return remember { DevInsAppState(scope) }
}
