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
import cc.unitmesh.agent.plan.AgentPlan
import cc.unitmesh.devins.idea.editor.*
import cc.unitmesh.devins.idea.toolwindow.plan.IdeaPlanSummaryBar
import cc.unitmesh.llm.NamedModelConfig
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import kotlinx.coroutines.Dispatchers
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
private val inputAreaLogger = Logger.getInstance("IdeaDevInInputArea")

/**
 * Helper function to build and send message with file references.
 * Extracts common logic from onSubmit and onSendClick.
 */
private fun buildAndSendMessage(
    text: String,
    selectedFiles: List<SelectedFileItem>,
    onSend: (String) -> Unit,
    clearInput: () -> Unit,
    clearFiles: () -> Unit
) {
    if (text.isBlank()) return

    val filesText = selectedFiles.joinToString("\n") { it.toDevInsCommand() }
    val fullText = if (filesText.isNotEmpty()) "$text\n$filesText" else text
    onSend(fullText)
    clearInput()
    clearFiles()
}

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
    onConfigureClick: () -> Unit = {},
    currentPlan: AgentPlan? = null
) {
    var inputText by remember { mutableStateOf("") }
    var devInInput by remember { mutableStateOf<IdeaDevInInput?>(null) }
    var selectedFiles by remember { mutableStateOf<List<SelectedFileItem>>(emptyList()) }
    var isEnhancing by remember { mutableStateOf(false) }

    // Use a ref to track current processing state for the SwingPanel listener
    val isProcessingRef = remember { mutableStateOf(isProcessing) }
    LaunchedEffect(isProcessing) { isProcessingRef.value = isProcessing }

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
        // Plan summary bar - shown above top toolbar when a plan is active
        IdeaPlanSummaryBar(
            plan = currentPlan,
            modifier = Modifier.fillMaxWidth()
        )

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
                            // Use ref to get current processing state
                            if (text.isNotBlank() && !isProcessingRef.value) {
                                buildAndSendMessage(
                                    text = text,
                                    selectedFiles = selectedFiles,
                                    onSend = onSend,
                                    clearInput = {
                                        clearInput()
                                        inputText = ""
                                    },
                                    clearFiles = { selectedFiles = emptyList() }
                                )
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
                    buildAndSendMessage(
                        text = text,
                        selectedFiles = selectedFiles,
                        onSend = onSend,
                        clearInput = {
                            devInInput?.clearInput()
                            inputText = ""
                        },
                        clearFiles = { selectedFiles = emptyList() }
                    )
                }
            },
            sendEnabled = inputText.isNotBlank() && !isProcessing,
            isExecuting = isProcessing,
            onStopClick = onAbort,
            onPromptOptimizationClick = {
                val currentText = devInInput?.text?.trim() ?: inputText.trim()
                inputAreaLogger.info("Prompt optimization clicked, text length: ${currentText.length}")

                if (currentText.isNotBlank() && !isEnhancing && !isProcessing) {
                    isEnhancing = true
                    scope.launch(Dispatchers.IO) {
                        try {
                            inputAreaLogger.info("Starting prompt enhancement...")
                            val enhancer = IdeaPromptEnhancer.getInstance(project)
                            val enhanced = enhancer.enhance(currentText)
                            inputAreaLogger.info("Enhancement completed, result length: ${enhanced.length}")

                            if (enhanced != currentText && enhanced.isNotBlank()) {
                                // Update UI on EDT using invokeLater
                                ApplicationManager.getApplication().invokeLater {
                                    devInInput?.replaceText(enhanced)
                                    inputText = enhanced
                                    inputAreaLogger.info("Text updated in input field")
                                }
                            } else {
                                inputAreaLogger.info("No enhancement made (same text or empty result)")
                            }
                        } catch (e: Exception) {
                            inputAreaLogger.error("Prompt enhancement failed: ${e.message}", e)
                        } finally {
                            ApplicationManager.getApplication().invokeLater {
                                isEnhancing = false
                            }
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