package cc.unitmesh.devins.idea.editor

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.devins.workspace.WorkspaceManager
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.ModelConfig
import cc.unitmesh.llm.NamedModelConfig
import cc.unitmesh.llm.PromptEnhancer
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import kotlinx.coroutines.launch
import org.jetbrains.jewel.ui.component.Text
import javax.swing.JPanel
import java.awt.BorderLayout

/**
 * DevIn Editor Input for IntelliJ IDEA.
 * 
 * Combines Swing EditorTextField with Compose toolbar for a hybrid UI.
 * Features:
 * - DevIn language support with syntax highlighting and completion
 * - Model configuration (left side of toolbar)
 * - MCP and prompt optimization (right side of toolbar)
 * - Integration with IntelliJ's completion system
 */
@Composable
fun IdeaDevInEditorInput(
    project: Project,
    disposable: Disposable,
    initialText: String = "",
    placeholder: String = "Type your message or /help for commands...",
    isExecuting: Boolean = false,
    onSubmit: (String) -> Unit = {},
    onStopClick: () -> Unit = {},
    totalTokens: Int? = null,
    modifier: Modifier = Modifier
) {
    // State management
    var showModelConfigDialog by remember { mutableStateOf(false) }
    var showMcpConfigDialog by remember { mutableStateOf(false) }
    var showPromptOptimizationDialog by remember { mutableStateOf(false) }
    
    var availableConfigs by remember { mutableStateOf<List<NamedModelConfig>>(emptyList()) }
    var currentConfigName by remember { mutableStateOf<String?>(null) }
    var currentModelConfig by remember { mutableStateOf<ModelConfig?>(null) }
    
    var llmService by remember { mutableStateOf<KoogLLMService?>(null) }
    var promptEnhancer by remember { mutableStateOf<PromptEnhancer?>(null) }
    
    val scope = rememberCoroutineScope()
    
    // Create the input component
    val inputComponent = remember {
        IdeaDevInInput(
            project = project,
            listeners = emptyList(),
            disposable = disposable,
            showAgent = true
        ).apply {
            text = initialText
        }
    }
    
    // Add submit listener
    DisposableEffect(Unit) {
        val listener = object : IdeaInputListener {
            override fun onSubmit(text: String, trigger: IdeaInputTrigger) {
                onSubmit(text)
                inputComponent.clearInput()
            }
            
            override fun onTextChanged(text: String) {
                // Handle text changes if needed
            }
            
            override fun editorAdded(editor: com.intellij.openapi.editor.ex.EditorEx) {
                // Handle editor added if needed
            }
        }
        
        inputComponent.addInputListener(listener)
        
        onDispose {
            inputComponent.removeInputListener(listener)
        }
    }
    
    // Load configurations
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val configWrapper = ConfigManager.load()
                availableConfigs = configWrapper.getAllConfigs()
                currentConfigName = configWrapper.getActiveName()
                currentModelConfig = configWrapper.getActiveModelConfig()

                // Initialize LLM service and prompt enhancer
                if (currentModelConfig != null && currentModelConfig!!.isValid()) {
                    llmService = KoogLLMService.create(currentModelConfig!!)

                    val workspace = WorkspaceManager.currentWorkspace
                    if (workspace != null) {
                        val fileSystem = workspace.fileSystem
                        val domainDictService = cc.unitmesh.indexer.DomainDictService(fileSystem)
                        promptEnhancer = PromptEnhancer(llmService!!, fileSystem, domainDictService)
                    }
                }
            } catch (e: Exception) {
                println("Failed to load configurations: ${e.message}")
            }
        }
    }
    
    Column(modifier = modifier) {
        // Swing editor panel
        SwingPanel(
            factory = {
                JPanel(BorderLayout()).apply {
                    add(inputComponent, BorderLayout.CENTER)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = 80.dp, max = 200.dp)
        )
        
        // Compose toolbar
        IdeaBottomToolbar(
            onSendClick = {
                val text = inputComponent.text.trim()
                if (text.isNotEmpty()) {
                    onSubmit(text)
                    inputComponent.clearInput()
                }
            },
            sendEnabled = inputComponent.text.isNotBlank(),
            isExecuting = isExecuting,
            onStopClick = onStopClick,
            onAtClick = {
                // Insert @ and trigger agent completion
                inputComponent.appendText("@")
            },
            onSlashClick = {
                // Insert / and trigger command completion
                inputComponent.appendText("/")
            },
            onSettingsClick = {
                showMcpConfigDialog = true
            },
            onPromptOptimizationClick = {
                showPromptOptimizationDialog = true
            },
            workspacePath = WorkspaceManager.currentWorkspace?.rootPath,
            totalTokens = totalTokens,
            availableConfigs = availableConfigs,
            currentConfigName = currentConfigName,
            onConfigSelect = { config ->
                scope.launch {
                    ConfigManager.setActive(config.name)
                    currentConfigName = config.name
                    currentModelConfig = config.toModelConfig()
                }
            },
            onConfigureClick = {
                showModelConfigDialog = true
            }
        )
    }

    // Model Configuration Dialog
    if (showModelConfigDialog) {
        // TODO: Create IDEA version of model configuration dialog
        // For now, just close it
        showModelConfigDialog = false
    }

    // MCP Configuration Dialog
    if (showMcpConfigDialog) {
        // TODO: Create IDEA version of MCP configuration dialog
        // Should migrate from mpp-ui/ToolConfigDialog.kt
        // For now, just close it
        showMcpConfigDialog = false
    }

    // Prompt Optimization Dialog
    if (showPromptOptimizationDialog) {
        // TODO: Create IDEA version of prompt optimization dialog
        // Should use PromptEnhancer to optimize the current input text
        // Reference: mpp-ui DevInEditorInput lines 319-349
        // For now, just close it
        showPromptOptimizationDialog = false
    }
}

