package cc.unitmesh.devins.idea.toolwindow

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import cc.unitmesh.agent.plan.AgentPlan
import cc.unitmesh.devins.idea.compose.rememberIdeaCoroutineScope
import cc.unitmesh.devins.idea.editor.*
import cc.unitmesh.llm.NamedModelConfig
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.BoxLayout
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

/**
 * Composable wrapper for SwingDevInInputArea.
 * Uses SwingPanel to embed the Swing-based input area in Compose.
 * This approach avoids z-index issues by keeping EditorTextField as native Swing
 * and using JewelComposePanel for Compose toolbars.
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
    onConfigureClick: () -> Unit = {},
    currentPlan: AgentPlan? = null
) {
    val scope = rememberIdeaCoroutineScope(project)
    var swingInputArea by remember { mutableStateOf<SwingDevInInputArea?>(null) }

    // Update SwingDevInInputArea properties when they change
    DisposableEffect(isProcessing) {
        swingInputArea?.setProcessing(isProcessing)
        onDispose { }
    }

    DisposableEffect(totalTokens) {
        swingInputArea?.setTotalTokens(totalTokens)
        onDispose { }
    }

    DisposableEffect(availableConfigs) {
        swingInputArea?.setAvailableConfigs(availableConfigs)
        onDispose { }
    }

    DisposableEffect(currentConfigName) {
        swingInputArea?.setCurrentConfigName(currentConfigName)
        onDispose { }
    }

    DisposableEffect(onConfigSelect) {
        swingInputArea?.setOnConfigSelect(onConfigSelect)
        onDispose { }
    }

    DisposableEffect(onConfigureClick) {
        swingInputArea?.setOnConfigureClick(onConfigureClick)
        onDispose { }
    }

    DisposableEffect(currentPlan) {
        swingInputArea?.setCurrentPlan(currentPlan)
        onDispose { }
    }

    // Embed SwingDevInInputArea using SwingPanel
    SwingPanel(
        modifier = Modifier.fillMaxSize(),
        factory = {
            SwingDevInInputArea(
                project = project,
                parentDisposable = parentDisposable,
                onSend = onSend,
                onAbort = onAbort,
                scope = scope
            ).also {
                swingInputArea = it
                // Apply initial values
                it.setProcessing(isProcessing)
                it.setTotalTokens(totalTokens)
                it.setAvailableConfigs(availableConfigs)
                it.setCurrentConfigName(currentConfigName)
                it.setOnConfigSelect(onConfigSelect)
                it.setOnConfigureClick(onConfigureClick)
                it.setCurrentPlan(currentPlan)
            }
        },
        update = { panel ->
            // Panel updates are handled via DisposableEffect above
        }
    )
}

/**
 * Pure Swing-based input area.
 * Uses native Swing toolbars to avoid z-index issues with Compose popups.
 */
class SwingDevInInputArea(
    private val project: Project,
    private val parentDisposable: Disposable,
    private val onSend: (String) -> Unit,
    private val onAbort: () -> Unit,
    private val scope: CoroutineScope
) : JPanel(BorderLayout()), Disposable {

    private val logger = Logger.getInstance(SwingDevInInputArea::class.java)

    private var devInInput: IdeaDevInInput? = null
    private var inputText: String = ""
    private var isProcessing: Boolean = false
    private var isEnhancing: Boolean = false

    // Swing toolbars
    private lateinit var topToolbar: SwingTopToolbar
    private lateinit var bottomToolbar: SwingBottomToolbar

    // Callbacks for config selection
    private var currentPlan: AgentPlan? = null

    init {
        border = JBUI.Borders.empty(0)
        setupUI()
        Disposer.register(parentDisposable, this)
    }

    private fun setupUI() {
        val contentPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty()
        }

        // Top toolbar (pure Swing)
        topToolbar = SwingTopToolbar(project) { files ->
            // Files selected callback - already handled in SwingTopToolbar
        }.apply {
            maximumSize = Dimension(Int.MAX_VALUE, 40)
        }
        contentPanel.add(topToolbar)

        // DevIn Editor (native Swing)
        val editorPanel = JPanel(BorderLayout()).apply {
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
                            sendMessage(text)
                        }
                    }

                    override fun onStop() {
                        onAbort()
                    }

                    override fun onTextChanged(text: String) {
                        inputText = text
                        bottomToolbar.setSendEnabled(text.isNotBlank() && !isProcessing)
                    }
                })
            }

            Disposer.register(parentDisposable, input)
            devInInput = input

            add(input, BorderLayout.CENTER)
            minimumSize = Dimension(200, 60)
            preferredSize = Dimension(Int.MAX_VALUE, 100)
        }
        contentPanel.add(editorPanel)

        // Bottom toolbar (pure Swing)
        bottomToolbar = SwingBottomToolbar(
            project = project,
            onSendClick = {
                val text = devInInput?.text?.trim() ?: inputText.trim()
                if (text.isNotBlank() && !isProcessing) {
                    sendMessage(text)
                }
            },
            onStopClick = onAbort,
            onPromptOptimizationClick = { handlePromptOptimization() }
        ).apply {
            maximumSize = Dimension(Int.MAX_VALUE, 40)
        }
        contentPanel.add(bottomToolbar)

        add(contentPanel, BorderLayout.CENTER)
    }

    private fun sendMessage(text: String) {
        val selectedFiles = topToolbar.getSelectedFiles()
        val filesText = selectedFiles.joinToString("\n") { it.toDevInsCommand() }
        val fullText = if (filesText.isNotEmpty()) "$text\n$filesText" else text
        onSend(fullText)
        devInInput?.clearInput()
        inputText = ""
        topToolbar.clearFiles()
        bottomToolbar.setSendEnabled(false)
    }

    private fun handlePromptOptimization() {
        val currentText = devInInput?.text?.trim() ?: inputText.trim()
        logger.info("Prompt optimization clicked, text length: ${currentText.length}")

        if (currentText.isNotBlank() && !isEnhancing && !isProcessing) {
            isEnhancing = true
            bottomToolbar.setEnhancing(true)

            scope.launch(Dispatchers.IO) {
                try {
                    logger.info("Starting prompt enhancement...")
                    val enhancer = IdeaPromptEnhancer.getInstance(project)
                    val enhanced = enhancer.enhance(currentText)
                    logger.info("Enhancement completed, result length: ${enhanced.length}")

                    if (enhanced != currentText && enhanced.isNotBlank()) {
                        ApplicationManager.getApplication().invokeLater {
                            devInInput?.replaceText(enhanced)
                            inputText = enhanced
                            logger.info("Text updated in input field")
                        }
                    } else {
                        logger.info("No enhancement made (same text or empty result)")
                    }
                } catch (e: Exception) {
                    logger.error("Prompt enhancement failed: ${e.message}", e)
                } finally {
                    ApplicationManager.getApplication().invokeLater {
                        isEnhancing = false
                        bottomToolbar.setEnhancing(false)
                    }
                }
            }
        }
    }

    fun setProcessing(processing: Boolean) {
        isProcessing = processing
        bottomToolbar.setProcessing(processing)
        bottomToolbar.setSendEnabled(inputText.isNotBlank() && !processing)
    }

    fun setTotalTokens(tokens: Int?) {
        bottomToolbar.setTotalTokens(tokens)
    }

    fun setAvailableConfigs(configs: List<NamedModelConfig>) {
        bottomToolbar.setAvailableConfigs(configs)
    }

    fun setCurrentConfigName(name: String?) {
        bottomToolbar.setCurrentConfigName(name)
    }

    fun setOnConfigSelect(callback: (NamedModelConfig) -> Unit) {
        bottomToolbar.setOnConfigSelect(callback)
    }

    fun setOnConfigureClick(callback: () -> Unit) {
        bottomToolbar.setOnConfigureClick(callback)
    }

    fun setCurrentPlan(plan: AgentPlan?) {
        currentPlan = plan
        // TODO: Add plan summary bar support
    }

    override fun dispose() {
        devInInput = null
    }
}