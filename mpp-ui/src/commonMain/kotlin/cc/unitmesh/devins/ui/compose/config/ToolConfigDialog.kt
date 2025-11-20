package cc.unitmesh.devins.ui.compose.config

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cc.unitmesh.agent.config.McpLoadingState
import cc.unitmesh.agent.config.McpLoadingStateCallback
import cc.unitmesh.agent.config.McpServerLoadingStatus
import cc.unitmesh.agent.config.McpServerState
import cc.unitmesh.agent.config.McpToolConfigManager
import cc.unitmesh.agent.config.ToolConfigFile
import cc.unitmesh.agent.config.ToolItem
import cc.unitmesh.agent.mcp.McpServerConfig
import cc.unitmesh.agent.tool.schema.ToolCategory
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.launch

@Composable
fun ToolConfigDialog(
    onDismiss: () -> Unit,
    onSave: (ToolConfigFile) -> Unit,
    llmService: KoogLLMService? = null
) {
    var toolConfig by remember { mutableStateOf(ToolConfigFile.default()) }
    // Built-in tools are now always enabled and not configurable via UI
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

    fun scheduleAutoSave() {
        hasUnsavedChanges = true
        autoSaveJob?.cancel()
                autoSaveJob =
            scope.launch {
                kotlinx.coroutines.delay(2000) // Wait 2 seconds before auto-saving
                try {
                    val enabledMcpTools =
                        mcpTools.values
                            .flatten()
                            .filter { it.enabled }
                            .map { it.name }

                    val result = deserializeMcpConfig(mcpConfigJson)
                    if (result.isSuccess) {
                        val newMcpServers = result.getOrThrow()
                        val updatedConfig =
                            toolConfig.copy(
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

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                toolConfig = ConfigManager.loadToolConfig()
                // Built-in tools are always enabled - no need to load them for configuration

                mcpConfigJson = serializeMcpConfig(toolConfig.mcpServers)

                if (toolConfig.mcpServers.isNotEmpty()) {
                    scope.launch {
                        // Create callback for incremental loading
                        val callback =
                            object : McpLoadingStateCallback {
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
                            mcpLoadingState =
                                McpToolConfigManager.discoverMcpToolsIncremental(
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

    DisposableEffect(Unit) {
        onDispose {
            autoSaveJob?.cancel()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier =
                Modifier
                    .width(850.dp)
                    .height(650.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
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
                            text = "Tool Configuration",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        if (hasUnsavedChanges) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        AutoDevComposeIcons.Schedule,
                                        contentDescription = "Auto-saving",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Auto-saving...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(AutoDevComposeIcons.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Tools") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("MCP Servers") }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (mcpLoadError != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    AutoDevComposeIcons.Error,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = mcpLoadError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    when (selectedTab) {
                        0 ->
                            ToolSelectionTab(
                                mcpTools = mcpTools,
                                mcpLoadingState = mcpLoadingState,
                                onMcpToolToggle = { toolName, enabled ->
                                    mcpTools =
                                        mcpTools.mapValues { (_, tools) ->
                                            tools.map { tool ->
                                                if (tool.name == toolName) tool.copy(enabled = enabled) else tool
                                            }
                                        }
                                    scheduleAutoSave()
                                }
                            )

                        1 ->
                            McpServerConfigTab(
                                mcpConfigJson = mcpConfigJson,
                                errorMessage = mcpConfigError,
                                isReloading = isReloading,
                                onMcpConfigChange = { newJson ->
                                    mcpConfigJson = newJson
                                    // Real-time JSON validation
                                    val result = deserializeMcpConfig(newJson)
                                    mcpConfigError =
                                        if (result.isFailure) {
                                            result.exceptionOrNull()?.message
                                        } else {
                                            null
                                        }
                                },
                                onReloadMcpTools = {
                                    scope.launch {
                                        try {
                                            isReloading = true
                                            mcpConfigError = null
                                            mcpLoadError = null

                                            val result = deserializeMcpConfig(mcpConfigJson)
                                            if (result.isFailure) {
                                                mcpConfigError =
                                                    result.exceptionOrNull()?.message ?: "Invalid JSON format"
                                                return@launch
                                            }

                                            val newMcpServers = result.getOrThrow()
                                            val updatedConfig = toolConfig.copy(mcpServers = newMcpServers)
                                            ConfigManager.saveToolConfig(updatedConfig)
                                            toolConfig = updatedConfig

                                            try {
                                                McpToolConfigManager.discoverMcpTools(
                                                    newMcpServers,
                                                    toolConfig.enabledMcpTools.toSet()
                                                )
                                                val totalTools = mcpTools.values.sumOf { it.size }
                                                println("✅ Reloaded $totalTools MCP tools from ${newMcpServers.size} servers")
                                            } catch (e: Exception) {
                                                mcpLoadError = "Failed to load MCP tools: ${e.message}"
                                                println("❌ Error loading MCP tools: ${e.message}")
                                            }
                                        } catch (e: Exception) {
                                            mcpConfigError = "Error reloading MCP tools: ${e.message}"
                                            println("❌ Error reloading MCP tools: ${e.message}")
                                        } finally {
                                            isReloading = false
                                        }
                                    }
                                }
                            )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Summary
                        val enabledMcp = mcpTools.values.flatten().count { it.enabled }
                        val totalMcp = mcpTools.values.flatten().size

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "MCP Tools: $enabledMcp/$totalMcp enabled | Built-in tools: Always enabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (hasUnsavedChanges) {
                                Text(
                                    text = "Changes will be auto-saved in 2 seconds...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    text = "All changes saved",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        autoSaveJob?.cancel()

                                        val enabledMcpTools =
                                            mcpTools.values
                                                .flatten()
                                                .filter { it.enabled }
                                                .map { it.name }

                                        // Parse MCP config from JSON
                                        val result = deserializeMcpConfig(mcpConfigJson)
                                        if (result.isFailure) {
                                            mcpConfigError = result.exceptionOrNull()?.message ?: "Invalid JSON format"
                                            selectedTab = 1 // Switch to MCP tab to show error
                                            return@launch
                                        }

                                        val newMcpServers = result.getOrThrow()

                                        val updatedConfig =
                                            toolConfig.copy(
                                                enabledMcpTools = enabledMcpTools,
                                                mcpServers = newMcpServers
                                            )

                                        ConfigManager.saveToolConfig(updatedConfig)
                                        onSave(updatedConfig)
                                        onDismiss()
                                    } catch (e: Exception) {
                                        println("Error saving tool config: ${e.message}")
                                    }
                                }
                            }
                        ) {
                            Text("Apply & Close")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolSelectionTab(
    mcpTools: Map<String, List<ToolItem>>,
    mcpLoadingState: McpLoadingState,
    onMcpToolToggle: (String, Boolean) -> Unit
) {
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        // Info banner about built-in tools
        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        AutoDevComposeIcons.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Built-in Tools Always Enabled",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "File operations, search, shell, and other essential tools are always available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Display MCP tools grouped by server with loading states
        val serverEntries = mcpLoadingState.servers.entries.toList()
        items(serverEntries) { (serverName, serverState) ->
            val serverKey = "MCP_SERVER_$serverName"
            val isServerExpanded = expandedCategories.getOrPut(serverKey) { true }
            val tools = mcpTools[serverName] ?: emptyList()

            McpServerHeader(
                serverName = serverName,
                serverState = serverState,
                tools = tools,
                isExpanded = isServerExpanded,
                onToggle = {
                    expandedCategories[serverKey] = !isServerExpanded
                }
            )

            if (isServerExpanded && tools.isNotEmpty()) {
                tools.forEach { tool ->
                    CompactToolItemRow(
                        tool = tool,
                        onToggle = { enabled ->
                            onMcpToolToggle(tool.name, enabled)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun McpServerConfigTab(
    mcpConfigJson: String,
    errorMessage: String?,
    isReloading: Boolean,
    onMcpConfigChange: (String) -> Unit,
    onReloadMcpTools: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MCP Server Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "JSON is validated in real-time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isReloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (errorMessage != null) {
                    Icon(
                        AutoDevComposeIcons.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Invalid JSON",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (mcpConfigJson.isNotBlank()) {
                    Icon(
                        AutoDevComposeIcons.CheckCircle,
                        contentDescription = "Valid",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Valid JSON",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (errorMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        AutoDevComposeIcons.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        OutlinedTextField(
            value = mcpConfigJson,
            onValueChange = onMcpConfigChange,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            textStyle =
                MaterialTheme.typography.bodySmall.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
            placeholder = {
                Text(
                    text = getDefaultMcpConfigTemplate(),
                    style = MaterialTheme.typography.bodySmall
                )
            },
            isError = errorMessage != null,
            enabled = !isReloading
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Example: uvx for Python tools, npx for Node.js tools",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onReloadMcpTools,
                enabled = !isReloading && errorMessage == null
            ) {
                if (isReloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(AutoDevComposeIcons.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isReloading) "Loading..." else "Save & Reload")
            }
        }
    }
}

@Composable
private fun McpServerHeader(
    serverName: String,
    serverState: McpServerState,
    tools: List<ToolItem>,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .clickable { onToggle() },
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (statusIcon, statusColor) =
                when (serverState.status) {
                    McpServerLoadingStatus.LOADING -> AutoDevComposeIcons.Refresh to MaterialTheme.colorScheme.primary
                    McpServerLoadingStatus.LOADED -> AutoDevComposeIcons.Cloud to MaterialTheme.colorScheme.primary
                    McpServerLoadingStatus.ERROR -> AutoDevComposeIcons.Error to MaterialTheme.colorScheme.error
                    McpServerLoadingStatus.DISABLED -> AutoDevComposeIcons.CloudOff to MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.6f
                    )
                    McpServerLoadingStatus.AVAILABLE -> AutoDevComposeIcons.CloudQueue to MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.6f
                    )
                }

            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MCP: $serverName",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
            }

            if (tools.isNotEmpty()) {
                Text(
                    text = "${tools.count { it.enabled }}/${tools.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(8.dp))
            }

            // Loading indicator
            if (serverState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Icon(
                imageVector = if (isExpanded) AutoDevComposeIcons.ExpandLess else AutoDevComposeIcons.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun CollapsibleCategoryHeader(
    category: ToolCategory? = null,
    categoryName: String? = null,
    icon: ImageVector,
    isExpanded: Boolean,
    toolCount: Int,
    enabledCount: Int,
    onToggle: () -> Unit
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onToggle() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isExpanded) AutoDevComposeIcons.ExpandMore else AutoDevComposeIcons.ChevronRight,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = categoryName ?: category?.name ?: "",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "$enabledCount/$toolCount",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CompactToolItemRow(
    tool: ToolItem,
    onToggle: (Boolean) -> Unit
) {
    var isChecked by remember { mutableStateOf(tool.enabled) }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 1.dp)
                .background(
                    color =
                        if (isChecked) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        } else {
                            Color.Transparent
                        },
                    shape = RoundedCornerShape(4.dp)
                )
                .clickable {
                    isChecked = !isChecked
                    onToggle(isChecked)
                }
                .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = {
                isChecked = it
                onToggle(it)
            },
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Name
        Text(
            text = tool.displayName,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(120.dp),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Description
        Text(
            text = tool.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )

        // MCP server badge (if applicable)
        if (tool.serverName.isNotEmpty()) {
            Spacer(modifier = Modifier.width(4.dp))
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                shape = RoundedCornerShape(2.dp)
            ) {
                Text(
                    text = tool.serverName,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                    fontSize = 9.sp
                )
            }
        }
    }
}

private fun getCategoryIcon(category: ToolCategory): ImageVector {
    return when (category) {
        ToolCategory.FileSystem -> AutoDevComposeIcons.Folder
        ToolCategory.Search -> AutoDevComposeIcons.Search
        ToolCategory.Execution -> AutoDevComposeIcons.PlayArrow
        ToolCategory.Information -> AutoDevComposeIcons.Info
        ToolCategory.Utility -> AutoDevComposeIcons.Build
        ToolCategory.SubAgent -> AutoDevComposeIcons.SmartToy
        ToolCategory.Communication -> AutoDevComposeIcons.Chat
    }
}

private fun serializeMcpConfig(mcpServers: Map<String, McpServerConfig>): String {
    if (mcpServers.isEmpty()) {
        return getDefaultMcpConfigTemplate()
    }

    return try {
        kotlinx.serialization.json.Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }.encodeToString(
            kotlinx.serialization.serializer<Map<String, McpServerConfig>>(),
            mcpServers
        )
    } catch (e: Exception) {
        println("Error serializing MCP config: ${e.message}")
        getDefaultMcpConfigTemplate()
    }
}

private fun deserializeMcpConfig(json: String): Result<Map<String, McpServerConfig>> {
    if (json.isBlank()) {
        return Result.success(emptyMap())
    }

    return try {
        val jsonParser =
            kotlinx.serialization.json.Json {
                prettyPrint = true
                ignoreUnknownKeys = true
                isLenient = true
            }

        val servers = jsonParser.decodeFromString<Map<String, McpServerConfig>>(json.trim())

        // Validate each server config
        servers.forEach { (name, config) ->
            if (!config.validate()) {
                return Result.failure(Exception("Invalid config for server '$name': must have either 'command' or 'url'"))
            }
        }

        Result.success(servers)
    } catch (e: Exception) {
        Result.failure(Exception("Failed to parse JSON: ${e.message}"))
    }
}

private fun getDefaultMcpConfigTemplate(): String {
    return """
{
  "filesystem": {
    "command": "npx",
    "args": ["-y", "@modelcontextprotocol/server-filesystem", "/tmp"],
    "env": {}
  },
  "github": {
    "command": "npx",
    "args": ["-y", "@modelcontextprotocol/server-github"],
    "env": {
      "GITHUB_TOKEN": "<your-token>"
    }
  }
}
        """.trimIndent()
}
