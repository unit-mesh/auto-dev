package cc.unitmesh.devins.idea.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.flow.distinctUntilChanged
import cc.unitmesh.agent.config.McpLoadingState
import cc.unitmesh.agent.config.McpLoadingStateCallback
import cc.unitmesh.agent.config.McpServerState
import cc.unitmesh.agent.config.McpToolConfigManager
import cc.unitmesh.agent.config.ToolConfigFile
import cc.unitmesh.agent.config.ToolItem
import cc.unitmesh.agent.mcp.McpServerConfig
import cc.unitmesh.devins.ui.config.ConfigManager
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.jewel.ui.component.*

// JSON serialization helpers
private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}

private fun serializeMcpConfig(servers: Map<String, McpServerConfig>): String {
    return try {
        json.encodeToString(servers)
    } catch (e: Exception) {
        "{}"
    }
}

private fun deserializeMcpConfig(jsonString: String): Result<Map<String, McpServerConfig>> {
    return try {
        val servers = json.decodeFromString<Map<String, McpServerConfig>>(jsonString)
        Result.success(servers)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/**
 * MCP Configuration Dialog for IntelliJ IDEA.
 * 
 * Features:
 * - Two tabs: Tools and MCP Servers
 * - Auto-save functionality (2 seconds delay)
 * - Real-time JSON validation
 * - Incremental MCP server loading
 * 
 * Migrated from mpp-ui/ToolConfigDialog.kt to use Jewel UI components.
 */
@Composable
fun IdeaMcpConfigDialog(
    onDismiss: () -> Unit
) {
    var toolConfig by remember { mutableStateOf(ToolConfigFile.default()) }
    var mcpTools by remember { mutableStateOf<Map<String, List<ToolItem>>>(emptyMap()) }
    var mcpLoadingState by remember { mutableStateOf(McpLoadingState()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }
    var mcpConfigJson by remember { mutableStateOf("") }
    var mcpConfigError by remember { mutableStateOf<String?>(null) }
    var mcpLoadError by remember { mutableStateOf<String?>(null) }
    var isReloading by remember { mutableStateOf(false) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var autoSaveJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val scope = rememberCoroutineScope()

    // Auto-save function
    fun scheduleAutoSave() {
        hasUnsavedChanges = true
        autoSaveJob?.cancel()
        autoSaveJob = scope.launch {
            kotlinx.coroutines.delay(2000) // Wait 2 seconds before auto-saving
            try {
                val enabledMcpTools = mcpTools.values
                    .flatten()
                    .filter { it.enabled }
                    .map { it.name }

                val result = deserializeMcpConfig(mcpConfigJson)
                if (result.isSuccess) {
                    val newMcpServers = result.getOrThrow()
                    val updatedConfig = toolConfig.copy(
                        enabledMcpTools = enabledMcpTools,
                        mcpServers = newMcpServers
                    )

                    ConfigManager.saveToolConfig(updatedConfig)
                    toolConfig = updatedConfig
                    hasUnsavedChanges = false
                    println("✅ Auto-saved tool configuration")
                }
            } catch (e: Exception) {
                println("❌ Auto-save failed: ${e.message}")
            }
        }
    }

    // Load configuration on startup
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                toolConfig = ConfigManager.loadToolConfig()
                mcpConfigJson = serializeMcpConfig(toolConfig.mcpServers)

                if (toolConfig.mcpServers.isNotEmpty()) {
                    scope.launch {
                        // Create callback for incremental loading
                        val callback = object : McpLoadingStateCallback {
                            override fun onServerStateChanged(serverName: String, state: McpServerState) {
                                mcpLoadingState = mcpLoadingState.updateServerState(serverName, state)

                                // Update tools when server is loaded
                                if (state.isLoaded) {
                                    mcpTools = mcpTools + (serverName to state.tools)
                                }
                            }

                            override fun onLoadingStateChanged(loadingState: McpLoadingState) {
                                mcpLoadingState = loadingState
                            }

                            override fun onBuiltinToolsLoaded(tools: List<ToolItem>) {
                                mcpLoadingState = mcpLoadingState.copy(builtinToolsLoaded = true)
                            }
                        }

                        try {
                            // Use incremental loading
                            mcpLoadingState = McpToolConfigManager.discoverMcpToolsIncremental(
                                toolConfig.mcpServers,
                                toolConfig.enabledMcpTools.toSet(),
                                callback
                            )
                            mcpLoadError = null
                        } catch (e: Exception) {
                            mcpLoadError = "Failed to load MCP tools: ${e.message}"
                            println("❌ Error loading MCP tools: ${e.message}")
                        }
                    }
                }

                isLoading = false
            } catch (e: Exception) {
                println("Error loading tool config: ${e.message}")
                mcpLoadError = "Failed to load configuration: ${e.message}"
                isLoading = false
            }
        }
    }

    // Cancel auto-save job on dispose
    DisposableEffect(Unit) {
        onDispose {
            autoSaveJob?.cancel()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(800.dp)
                .height(600.dp)
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Tool Configuration")
                    if (hasUnsavedChanges) {
                        Text("(Auto-saving...)", color = org.jetbrains.jewel.foundation.theme.JewelTheme.globalColors.text.info)
                    }
                }
                IconButton(onClick = onDismiss) {
                    Text("×")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading...")
                }
            } else {
                // Tab Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DefaultButton(
                        onClick = { selectedTab = 0 },
                        enabled = selectedTab != 0
                    ) {
                        Text("Tools")
                    }
                    DefaultButton(
                        onClick = { selectedTab = 1 },
                        enabled = selectedTab != 1
                    ) {
                        Text("MCP Servers")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Error message
                mcpLoadError?.let { error ->
                    Text(error, color = org.jetbrains.jewel.foundation.theme.JewelTheme.globalColors.text.error)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Tab content
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> McpToolsTab(
                            mcpTools = mcpTools,
                            mcpLoadingState = mcpLoadingState,
                            onToolToggle = { toolName, enabled ->
                                mcpTools = mcpTools.mapValues { (_, tools) ->
                                    tools.map { tool ->
                                        if (tool.name == toolName) tool.copy(enabled = enabled) else tool
                                    }
                                }
                                scheduleAutoSave()
                            }
                        )
                        1 -> McpServersTab(
                            mcpConfigJson = mcpConfigJson,
                            errorMessage = mcpConfigError,
                            isReloading = isReloading,
                            onConfigChange = { newJson ->
                                mcpConfigJson = newJson
                                val result = deserializeMcpConfig(newJson)
                                mcpConfigError = if (result.isFailure) {
                                    result.exceptionOrNull()?.message
                                } else {
                                    null
                                }
                                scheduleAutoSave()
                            },
                            onReload = {
                                scope.launch {
                                    isReloading = true
                                    val result = deserializeMcpConfig(mcpConfigJson)
                                    if (result.isSuccess) {
                                        val newServers = result.getOrThrow()
                                        toolConfig = toolConfig.copy(mcpServers = newServers)
                                        ConfigManager.saveToolConfig(toolConfig)
                                        // Reload MCP tools
                                        try {
                                            val callback = object : McpLoadingStateCallback {
                                                override fun onServerStateChanged(serverName: String, state: McpServerState) {
                                                    mcpLoadingState = mcpLoadingState.updateServerState(serverName, state)
                                                    if (state.isLoaded) {
                                                        mcpTools = mcpTools + (serverName to state.tools)
                                                    }
                                                }
                                                override fun onLoadingStateChanged(loadingState: McpLoadingState) {
                                                    mcpLoadingState = loadingState
                                                }
                                                override fun onBuiltinToolsLoaded(tools: List<ToolItem>) {
                                                    mcpLoadingState = mcpLoadingState.copy(builtinToolsLoaded = true)
                                                }
                                            }
                                            mcpLoadingState = McpToolConfigManager.discoverMcpToolsIncremental(
                                                newServers,
                                                toolConfig.enabledMcpTools.toSet(),
                                                callback
                                            )
                                            mcpLoadError = null
                                        } catch (e: Exception) {
                                            mcpLoadError = "Failed to reload: ${e.message}"
                                        }
                                    }
                                    isReloading = false
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val enabledMcp = mcpTools.values.flatten().count { it.enabled }
                    val totalMcp = mcpTools.values.flatten().size
                    Text("MCP Tools: $enabledMcp/$totalMcp enabled")

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onDismiss) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun McpToolsTab(
    mcpTools: Map<String, List<ToolItem>>,
    mcpLoadingState: McpLoadingState,
    onToolToggle: (String, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        mcpTools.forEach { (serverName, tools) ->
            item {
                Text(serverName, modifier = Modifier.padding(vertical = 4.dp))
            }
            items(tools) { tool ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tool.displayName)
                        Text(tool.description, color = org.jetbrains.jewel.foundation.theme.JewelTheme.globalColors.text.info)
                    }
                    Checkbox(
                        checked = tool.enabled,
                        onCheckedChange = { onToolToggle(tool.name, it) }
                    )
                }
            }
        }

        val isLoading = mcpLoadingState.loadingServers.isNotEmpty()

        if (mcpTools.isEmpty() && !isLoading) {
            item {
                Text("No MCP tools configured. Add MCP servers in the 'MCP Servers' tab.")
            }
        }

        if (isLoading) {
            item {
                Text("Loading MCP tools...")
            }
        }
    }
}

@Composable
private fun McpServersTab(
    mcpConfigJson: String,
    errorMessage: String?,
    isReloading: Boolean,
    onConfigChange: (String) -> Unit,
    onReload: () -> Unit
) {
    val textFieldState = rememberTextFieldState(mcpConfigJson)

    // Sync text field state to callback
    LaunchedEffect(Unit) {
        snapshotFlow { textFieldState.text.toString() }
            .distinctUntilChanged()
            .collect { newText ->
                if (newText != mcpConfigJson) {
                    onConfigChange(newText)
                }
            }
    }

    // Update text field when external value changes
    LaunchedEffect(mcpConfigJson) {
        if (textFieldState.text.toString() != mcpConfigJson) {
            textFieldState.setTextAndPlaceCursorAtEnd(mcpConfigJson)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("MCP Server Configuration (JSON)")

        errorMessage?.let { error ->
            Text(error, color = org.jetbrains.jewel.foundation.theme.JewelTheme.globalColors.text.error)
        }

        // Use BasicTextField for multi-line text input
        BasicTextField(
            state = textFieldState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            textStyle = TextStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = org.jetbrains.jewel.foundation.theme.JewelTheme.globalColors.text.normal
            ),
            cursorBrush = SolidColor(org.jetbrains.jewel.foundation.theme.JewelTheme.globalColors.text.normal)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            DefaultButton(
                onClick = onReload,
                enabled = !isReloading && errorMessage == null
            ) {
                Text(if (isReloading) "Reloading..." else "Reload MCP Tools")
            }
        }
    }
}

