package cc.unitmesh.devins.ui.compose.state

import androidx.compose.runtime.*
import cc.unitmesh.devins.compiler.DevInsCompilerFacade
import cc.unitmesh.devins.filesystem.DefaultFileSystem
import cc.unitmesh.devins.filesystem.ProjectFileSystem
import cc.unitmesh.devins.ui.platform.createFileChooser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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

    var currentFilePath by mutableStateOf<String?>(null)
        private set

    var projectRootPath by mutableStateOf<String?>(null)
        private set

    // 动态创建 fileSystem，当 projectRootPath 改变时更新
    var fileSystem by mutableStateOf<ProjectFileSystem?>(null)
        private set

    var isCompiling by mutableStateOf(false)
        private set

    val canCompile: Boolean
        get() = editorContent.isNotBlank() && !isCompiling

    val showFileTree: Boolean
        get() = projectRootPath != null

    val showOutput: Boolean
        get() = output.isNotBlank()

    fun updateEditorContent(content: String) {
        editorContent = content
    }

    fun openFile() {
        scope.launch {
            val fileChooser = createFileChooser()
            val selectedPath =
                fileChooser.chooseFile(
                    title = "Open File",
                    initialDirectory = currentFilePath?.substringBeforeLast('/'),
                    fileExtensions = listOf("devin", "devins", "kt", "java", "js", "ts", "py", "json", "yaml", "yml", "md", "txt")
                )

            selectedPath?.let { openFileInEditor(it) }
        }
    }

    fun openProject() {
        scope.launch {
            val fileChooser = createFileChooser()
            val selectedPath =
                fileChooser.chooseDirectory(
                    title = "Select Project Directory",
                    initialDirectory = projectRootPath
                )

            selectedPath?.let { path ->
                projectRootPath = path
                fileSystem = DefaultFileSystem(path)
                val projectName = path.substringAfterLast('/')
                statusMessage = "Opened project: $projectName"
            }
        }
    }

    fun openFileInEditor(filePath: String) {
        try {
            currentFilePath = filePath
            editorContent = fileSystem?.readFile(filePath) ?: ""
            val fileName = filePath.substringAfterLast('/')
            statusMessage = "Opened: $fileName"
        } catch (e: Exception) {
            statusMessage = "Error opening file: ${e.message}"
        }
    }

    fun saveCurrentFile() {
        currentFilePath?.let { path ->
            try {
                val success = fileSystem?.writeFile(path, editorContent) ?: false
                val fileName = path.substringAfterLast('/')
                if (success) {
                    statusMessage = "Saved: $fileName"
                } else {
                    statusMessage = "Failed to save: $fileName"
                }
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
                val result = DevInsCompilerFacade.compile(editorContent)

                if (result.isSuccess()) {
                    output = result.output
                    isOutputError = false
                    statusMessage = "Compilation successful - " +
                        "Variables: ${result.statistics.variableCount}, " +
                        "Commands: ${result.statistics.commandCount}, " +
                        "Agents: ${result.statistics.agentCount}"
                } else {
                    output = "Error: ${result.errorMessage}"
                    isOutputError = true
                    statusMessage = "Compilation failed"
                }
            } catch (e: Exception) {
                output = "Exception: ${e.message}"
                isOutputError = true
                statusMessage = "Compilation error"
            } finally {
                isCompiling = false
            }
        }
    }

    fun clearOutput() {
        output = ""
        isOutputError = false
        statusMessage = "Output cleared"
    }

    private fun getDefaultContent(): String =
        """
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
fun rememberDevInsAppState(scope: CoroutineScope = rememberCoroutineScope()): DevInsAppState {
    return remember { DevInsAppState(scope) }
}
