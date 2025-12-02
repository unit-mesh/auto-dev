package cc.unitmesh.devins.idea.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.flow.distinctUntilChanged
import cc.unitmesh.agent.config.McpLoadingState
import cc.unitmesh.agent.config.McpLoadingStateCallback
import cc.unitmesh.agent.config.McpServerState
import cc.unitmesh.agent.config.McpToolConfigManager
import cc.unitmesh.agent.config.ToolConfigFile
import cc.unitmesh.agent.config.ToolItem
import cc.unitmesh.agent.mcp.McpServerConfig
import cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons
import cc.unitmesh.devins.ui.config.ConfigManager
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.jewel.foundation.theme.JewelTheme
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
 * Styled to match IdeaModelConfigDialog for consistency.
 */
@Composable
fun IdeaMcpConfigDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        IdeaMcpConfigDialogContent(onDismiss = onDismiss)
    }
}

/**
 * Content for the MCP configuration dialog.
 * Extracted to be used both in Compose Dialog and DialogWrapper.
 */
@Composable
fun IdeaMcpConfigDialogContent(
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
            kotlinx.coroutines.delay(2000)
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
                }
            } catch (e: Exception) {
                // Silent fail for auto-save
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

                        try {
                            mcpLoadingState = McpToolConfigManager.discoverMcpToolsIncremental(
                                toolConfig.mcpServers,
                                toolConfig.enabledMcpTools.toSet(),
                                callback
                            )
                            mcpLoadError = null
                        } catch (e: Exception) {
                            mcpLoadError = "Failed to load MCP tools: ${e.message}"
                        }
                    }
                }

                isLoading = false
            } catch (e: Exception) {
                mcpLoadError = "Failed to load configuration: ${e.message}"
                isLoading = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            autoSaveJob?.cancel()
        }
    }

    Box(
        modifier = Modifier
            .width(600.dp)
            .heightIn(max = 700.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(JewelTheme.globalColors.panelBackground)
            .onKeyEvent { event ->
                if (event.key == Key.Escape) {
                    onDismiss()
                    true
                } else false
            }
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            // Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "MCP Configuration",
                        style = JewelTheme.defaultTextStyle.copy(fontSize = 18.sp)
                    )
                    if (hasUnsavedChanges) {
                        Text(
                            text = "(Saving...)",
                            style = JewelTheme.defaultTextStyle.copy(
                                fontSize = 12.sp,
                                color = JewelTheme.globalColors.text.info
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading configuration...")
                }
            } else {
                // Tab selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    McpTabButton(
                        text = "Tools",
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    McpTabButton(
                        text = "MCP Servers",
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Error message
                mcpLoadError?.let { error ->
                    Text(
                        text = error,
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 12.sp,
                            color = JewelTheme.globalColors.text.error
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Tab content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
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

                Spacer(modifier = Modifier.height(16.dp))

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val enabledMcp = mcpTools.values.flatten().count { it.enabled }
                    val totalMcp = mcpTools.values.flatten().size
                    Text(
                        text = "MCP Tools: $enabledMcp/$totalMcp enabled",
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 12.sp,
                            color = JewelTheme.globalColors.text.normal.copy(alpha = 0.7f)
                        )
                    )

                    OutlinedButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun McpTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (selected) JewelTheme.globalColors.borders.normal.copy(alpha = 0.3f)
                else androidx.compose.ui.graphics.Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = JewelTheme.defaultTextStyle.copy(
                fontSize = 13.sp,
                color = if (selected) JewelTheme.globalColors.text.normal
                else JewelTheme.globalColors.text.normal.copy(alpha = 0.7f)
            )
        )
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

