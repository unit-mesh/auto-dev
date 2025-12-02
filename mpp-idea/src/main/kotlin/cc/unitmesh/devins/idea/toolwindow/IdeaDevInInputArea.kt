package cc.unitmesh.devins.idea.toolwindow

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.idea.editor.*
import cc.unitmesh.llm.NamedModelConfig
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JPanel

/**
 * Advanced chat input area with full DevIn language support.
 *
 * Uses IdeaDevInInput (EditorTextField-based) embedded via SwingPanel for:
 * - DevIn language syntax highlighting and completion
 * - IntelliJ's native completion popup integration
 * - Enter to submit, Shift+Enter for newline
 * - @ trigger for agent completion
 * - Token usage display
 * - Settings access
 * - Stop/Send button based on execution state
 * - Model selector for switching between LLM configurations
 *
 * Layout: Unified border around the entire input area for a cohesive look.
 */
@Composable
fun IdeaDevInInputArea(
    project: Project,
    parentDisposable: Disposable,
    isProcessing: Boolean,
    onSend: (String) -> Unit,
    onAbort: () -> Unit,
    workspacePath: String? = null,
    totalTokens: Int? = null,
    onAtClick: () -> Unit = {},
    availableConfigs: List<NamedModelConfig> = emptyList(),
    currentConfigName: String? = null,
    onConfigSelect: (NamedModelConfig) -> Unit = {},
    onConfigureClick: () -> Unit = {}
) {
    var inputText by remember { mutableStateOf("") }
    var devInInput by remember { mutableStateOf<IdeaDevInInput?>(null) }
    var selectedFiles by remember { mutableStateOf<List<SelectedFileItem>>(emptyList()) }
    var isEnhancing by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val borderShape = RoundedCornerShape(8.dp)

    // Outer container with unified border
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .clip(borderShape)
            .border(
                width = 1.dp,
                color = JewelTheme.globalColors.borders.normal,
                shape = borderShape
            )
    ) {
        // Top toolbar with file selection (no individual border)
        IdeaTopToolbar(
            project = project,
            onAtClick = onAtClick,
            selectedFiles = selectedFiles,
            onRemoveFile = { file ->
                selectedFiles = selectedFiles.filter { it.path != file.path }
            },
            onFilesSelected = { files ->
                val newItems = files.map { vf ->
                    SelectedFileItem(
                        name = vf.name,
                        path = vf.path,
                        virtualFile = vf,
                        isDirectory = vf.isDirectory
                    )
                }
                selectedFiles = (selectedFiles + newItems).distinctBy { it.path }
            }
        )

        // DevIn Editor via SwingPanel - uses weight(1f) to fill available space
        SwingPanel(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            factory = {
                val input = IdeaDevInInput(
                    project = project,
                    disposable = parentDisposable,
                    showAgent = true
                ).apply {
                    recreateDocument()

                    addInputListener(object : IdeaInputListener {
                        override fun editorAdded(editor: EditorEx) {
                            // Editor is ready
                        }

                        override fun onSubmit(text: String, trigger: IdeaInputTrigger) {
                            if (text.isNotBlank() && !isProcessing) {
                                // Append file references to the message (use /dir: for directories, /file: for files)
                                val filesText = selectedFiles.joinToString("\n") { it.toDevInsCommand() }
                                val fullText = if (filesText.isNotEmpty()) {
                                    "$text\n$filesText"
                                } else {
                                    text
                                }
                                onSend(fullText)
                                clearInput()
                                inputText = ""
                                // Clear selected files after sending
                                selectedFiles = emptyList()
                            }
                        }

                        override fun onStop() {
                            onAbort()
                        }

                        override fun onTextChanged(text: String) {
                            inputText = text
                        }
                    })
                }

                // Register for disposal
                Disposer.register(parentDisposable, input)
                devInInput = input

                // Wrap in a JPanel to handle dynamic sizing
                JPanel(BorderLayout()).apply {
                    add(input, BorderLayout.CENTER)
                    // Don't set fixed preferredSize - let it fill available space
                    minimumSize = Dimension(200, 60)
                }
            },
            update = { panel ->
                // Update panel if needed
            }
        )

        // Bottom toolbar with Compose (no individual border)
        IdeaBottomToolbar(
            project = project,
            onSendClick = {
                val text = devInInput?.text?.trim() ?: inputText.trim()
                if (text.isNotBlank() && !isProcessing) {
                    // Append file references to the message (use /dir: for directories, /file: for files)
                    val filesText = selectedFiles.joinToString("\n") { it.toDevInsCommand() }
                    val fullText = if (filesText.isNotEmpty()) {
                        "$text\n$filesText"
                    } else {
                        text
                    }
                    onSend(fullText)
                    devInInput?.clearInput()
                    inputText = ""
                    // Clear selected files after sending
                    selectedFiles = emptyList()
                }
            },
            sendEnabled = inputText.isNotBlank() && !isProcessing,
            isExecuting = isProcessing,
            onStopClick = onAbort,
            onPromptOptimizationClick = {
                val currentText = devInInput?.text?.trim() ?: inputText.trim()
                if (currentText.isNotBlank() && !isEnhancing && !isProcessing) {
                    isEnhancing = true
                    scope.launch {
                        try {
                            val enhancer = IdeaPromptEnhancer.getInstance(project)
                            val enhanced = enhancer.enhance(currentText)
                            if (enhanced != currentText) {
                                devInInput?.setText(enhanced)
                                inputText = enhanced
                            }
                        } catch (e: Exception) {
                            // Silently fail - keep original text
                        } finally {
                            isEnhancing = false
                        }
                    }
                }
            },
            isEnhancing = isEnhancing,
            totalTokens = totalTokens,
            availableConfigs = availableConfigs,
            currentConfigName = currentConfigName,
            onConfigSelect = onConfigSelect,
            onConfigureClick = onConfigureClick
        )
    }
}