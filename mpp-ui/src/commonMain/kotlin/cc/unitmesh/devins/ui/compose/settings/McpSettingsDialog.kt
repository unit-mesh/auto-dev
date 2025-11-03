package cc.unitmesh.devins.ui.compose.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cc.unitmesh.agent.mcp.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

/**
 * JSON structure for importing MCP server configuration
 */
@Serializable
data class McpServersJson(
    val mcpServers: Map<String, McpServerConfig>
)

/**
 * MCP Settings Dialog
 *
 * Allows users to:
 * 1. View configured MCP servers
 * 2. Import MCP servers from JSON
 * 3. Add/edit/remove MCP servers
 * 4. View discovered tools from each server
 * 5. Enable/disable tools
 * 6. See server connection status
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McpSettingsDialog(
    mcpClientManager: McpClientManager,
    initialServers: Map<String, McpServerConfig> = emptyMap(),
    onDismiss: () -> Unit,
    onSave: (Map<String, McpServerConfig>, Map<String, List<McpToolInfo>>) -> Unit
) {
    var servers by remember { mutableStateOf(initialServers) }
    var discoveredTools by remember { mutableStateOf<Map<String, List<McpToolInfo>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTools by remember { mutableStateOf<Map<String, Set<String>>>(emptyMap()) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showAddServerDialog by remember { mutableStateOf(false) }
    var editingServer by remember { mutableStateOf<Pair<String, McpServerConfig>?>(null) }
    var deletingServer by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Function to reload tools
    fun reloadTools() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                // Reinitialize with current servers
                mcpClientManager.initialize(McpConfig(mcpServers = servers))
                val tools = mcpClientManager.discoverAllTools()
                discoveredTools = tools
            } catch (e: Exception) {
                errorMessage = "Failed to discover tools: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Load tools on first composition
    LaunchedEffect(servers) {
        if (servers.isNotEmpty()) {
            reloadTools()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .width(700.dp)
                .heightIn(max = 600.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MCP Tools Configuration",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Import JSON button
                        IconButton(
                            onClick = { showImportDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Upload,
                                contentDescription = "Import JSON",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Add server button
                        IconButton(
                            onClick = { showAddServerDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Server",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Close button
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator()
                                Text("Discovering MCP tools...")
                            }
                        }
                    }

                    errorMessage != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = errorMessage!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Button(onClick = { reloadTools() }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }

                    servers.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = "No servers",
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "No MCP servers configured",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Click the + button to add a server or import from JSON",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    else -> {
                        // Tools list
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            discoveredTools.forEach { (serverName, tools) ->
                                item {
                                    McpServerSection(
                                        serverName = serverName,
                                        serverConfig = servers[serverName] ?: McpServerConfig(),
                                        tools = tools,
                                        serverStatus = mcpClientManager.getServerStatus(serverName),
                                        selectedToolNames = selectedTools[serverName] ?: emptySet(),
                                        onToolToggle = { toolName, selected ->
                                            selectedTools = selectedTools.toMutableMap().apply {
                                                val current = this[serverName]?.toMutableSet() ?: mutableSetOf()
                                                if (selected) {
                                                    current.add(toolName)
                                                } else {
                                                    current.remove(toolName)
                                                }
                                                this[serverName] = current
                                            }
                                        },
                                        onEdit = {
                                            editingServer = serverName to (servers[serverName] ?: McpServerConfig())
                                        },
                                        onDelete = {
                                            deletingServer = serverName
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text("Cancel")
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    // Convert selected tools to enabled tools
                                    val enabledTools = discoveredTools.mapValues { (serverName, tools) ->
                                        val selected = selectedTools[serverName] ?: emptySet()
                                        tools.map { tool ->
                                            tool.copy(enabled = selected.contains(tool.name))
                                        }
                                    }
                                    onSave(servers, enabledTools)
                                    onDismiss()
                                }
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save")
                            }
                        }
                    }
                }
            }
        }
    }

    // Import JSON Dialog
    if (showImportDialog) {
        ImportJsonDialog(
            onDismiss = { showImportDialog = false },
            onImport = { importedServers ->
                servers = servers + importedServers
                showImportDialog = false
                reloadTools()
            }
        )
    }

    // Add Server Dialog
    if (showAddServerDialog) {
        AddServerDialog(
            onDismiss = { showAddServerDialog = false },
            onAdd = { name, config ->
                servers = servers + (name to config)
                showAddServerDialog = false
                reloadTools()
            }
        )
    }

    // Edit Server Dialog
    editingServer?.let { (name, config) ->
        EditServerDialog(
            serverName = name,
            initialConfig = config,
            onDismiss = { editingServer = null },
            onSave = { updatedConfig ->
                servers = servers + (name to updatedConfig)
                editingServer = null
                reloadTools()
            }
        )
    }

    // Delete Confirmation Dialog
    deletingServer?.let { name ->
        DeleteConfirmationDialog(
            serverName = name,
            onDismiss = { deletingServer = null },
            onConfirm = {
                servers = servers - name
                deletingServer = null
                reloadTools()
            }
        )
    }
}

@Composable
private fun McpServerSection(
    serverName: String,
    serverConfig: McpServerConfig,
    tools: List<McpToolInfo>,
    serverStatus: McpServerStatus,
    selectedToolNames: Set<String>,
    onToolToggle: (String, Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Server header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (serverStatus) {
                            McpServerStatus.CONNECTED -> Icons.Default.CheckCircle
                            McpServerStatus.CONNECTING -> Icons.Default.Refresh
                            McpServerStatus.DISCONNECTING -> Icons.Default.Refresh
                            McpServerStatus.DISCONNECTED -> Icons.Default.Error
                        },
                        contentDescription = serverStatus.name,
                        tint = when (serverStatus) {
                            McpServerStatus.CONNECTED -> MaterialTheme.colorScheme.primary
                            McpServerStatus.CONNECTING -> MaterialTheme.colorScheme.secondary
                            McpServerStatus.DISCONNECTING -> MaterialTheme.colorScheme.secondary
                            McpServerStatus.DISCONNECTED -> MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.size(20.dp)
                    )

                    Text(
                        text = serverName,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "(${tools.size} tools)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit server",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete server",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand"
                        )
                    }
                }
            }

            // Tools list
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))

                tools.forEach { tool ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedToolNames.contains(tool.name),
                            onCheckedChange = { checked ->
                                onToolToggle(tool.name, checked)
                            }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = tool.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (tool.description.isNotEmpty()) {
                                Text(
                                    text = tool.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog for importing MCP servers from JSON
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportJsonDialog(
    onDismiss: () -> Unit,
    onImport: (Map<String, McpServerConfig>) -> Unit
) {
    var jsonText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .width(600.dp)
                .heightIn(max = 500.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Import MCP Servers from JSON",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Help text
                Text(
                    text = "Paste your MCP server configuration JSON below:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Example
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Example:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = """
                                {
                                  "mcpServers": {
                                    "AutoDev": {
                                      "command": "npx",
                                      "args": ["-y", "@jetbrains/mcp-proxy"],
                                      "disabled": false,
                                      "autoApprove": []
                                    }
                                  }
                                }
                            """.trimIndent(),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // JSON input
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = {
                        jsonText = it
                        errorMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    placeholder = { Text("Paste JSON here...") },
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            try {
                                val json = Json { ignoreUnknownKeys = true }
                                val parsed = json.decodeFromString<McpServersJson>(jsonText)
                                onImport(parsed.mcpServers)
                            } catch (e: Exception) {
                                errorMessage = "Invalid JSON: ${e.message}"
                            }
                        },
                        enabled = jsonText.isNotBlank()
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import")
                    }
                }
            }
        }
    }
}

/**
 * Dialog for adding a new MCP server
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddServerDialog(
    onDismiss: () -> Unit,
    onAdd: (String, McpServerConfig) -> Unit
) {
    var serverName by remember { mutableStateOf("") }
    var transportType by remember { mutableStateOf("stdio") } // "stdio" or "sse"
    var command by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var argsText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .width(500.dp)
                .heightIn(max = 600.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add MCP Server",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Server name
                    OutlinedTextField(
                        value = serverName,
                        onValueChange = {
                            serverName = it
                            errorMessage = null
                        },
                        label = { Text("Server Name") },
                        placeholder = { Text("e.g., AutoDev") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Transport type selector
                    Text(
                        text = "Transport Type",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = transportType == "stdio",
                            onClick = {
                                transportType = "stdio"
                                errorMessage = null
                            },
                            label = { Text("Stdio (Command)") }
                        )
                        FilterChip(
                            selected = transportType == "sse",
                            onClick = {
                                transportType = "sse"
                                errorMessage = null
                            },
                            label = { Text("SSE (URL)") }
                        )
                    }

                    // Conditional fields based on transport type
                    if (transportType == "stdio") {
                        // Command
                        OutlinedTextField(
                            value = command,
                            onValueChange = {
                                command = it
                                errorMessage = null
                            },
                            label = { Text("Command") },
                            placeholder = { Text("e.g., npx") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Arguments
                        OutlinedTextField(
                            value = argsText,
                            onValueChange = {
                                argsText = it
                                errorMessage = null
                            },
                            label = { Text("Arguments (one per line)") },
                            placeholder = { Text("-y\n@jetbrains/mcp-proxy") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 5
                        )

                        // Help text
                        Text(
                            text = "Enter each argument on a separate line",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // URL
                        OutlinedTextField(
                            value = url,
                            onValueChange = {
                                url = it
                                errorMessage = null
                            },
                            label = { Text("Server URL") },
                            placeholder = { Text("e.g., http://localhost:3000/sse") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Help text
                        Text(
                            text = "Enter the SSE endpoint URL",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Error message
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            when {
                                serverName.isBlank() -> errorMessage = "Server name is required"
                                transportType == "stdio" && command.isBlank() -> errorMessage = "Command is required"
                                transportType == "sse" && url.isBlank() -> errorMessage = "URL is required"
                                else -> {
                                    val config = if (transportType == "stdio") {
                                        val args = argsText.lines()
                                            .map { it.trim() }
                                            .filter { it.isNotBlank() }

                                        McpServerConfig(
                                            command = command.trim(),
                                            args = args,
                                            disabled = false,
                                            autoApprove = emptyList()
                                        )
                                    } else {
                                        McpServerConfig(
                                            url = url.trim(),
                                            disabled = false,
                                            autoApprove = emptyList()
                                        )
                                    }
                                    onAdd(serverName.trim(), config)
                                }
                            }
                        },
                        enabled = serverName.isNotBlank() &&
                                  ((transportType == "stdio" && command.isNotBlank()) ||
                                   (transportType == "sse" && url.isNotBlank()))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add")
                    }
                }
            }
        }
    }
}

/**
 * Dialog for editing an existing MCP server
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditServerDialog(
    serverName: String,
    initialConfig: McpServerConfig,
    onDismiss: () -> Unit,
    onSave: (McpServerConfig) -> Unit
) {
    var transportType by remember {
        mutableStateOf(if (initialConfig.isStdioTransport()) "stdio" else "sse")
    }
    var command by remember { mutableStateOf(initialConfig.command ?: "") }
    var url by remember { mutableStateOf(initialConfig.url ?: "") }
    var argsText by remember {
        mutableStateOf(initialConfig.args.joinToString("\n"))
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .width(500.dp)
                .heightIn(max = 600.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Edit MCP Server",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = serverName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Transport type selector
                    Text(
                        text = "Transport Type",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = transportType == "stdio",
                            onClick = {
                                transportType = "stdio"
                                errorMessage = null
                            },
                            label = { Text("Stdio (Command)") }
                        )
                        FilterChip(
                            selected = transportType == "sse",
                            onClick = {
                                transportType = "sse"
                                errorMessage = null
                            },
                            label = { Text("SSE (URL)") }
                        )
                    }

                    // Conditional fields based on transport type
                    if (transportType == "stdio") {
                        // Command
                        OutlinedTextField(
                            value = command,
                            onValueChange = {
                                command = it
                                errorMessage = null
                            },
                            label = { Text("Command") },
                            placeholder = { Text("e.g., npx") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Arguments
                        OutlinedTextField(
                            value = argsText,
                            onValueChange = {
                                argsText = it
                                errorMessage = null
                            },
                            label = { Text("Arguments (one per line)") },
                            placeholder = { Text("-y\n@jetbrains/mcp-proxy") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 5
                        )

                        // Help text
                        Text(
                            text = "Enter each argument on a separate line",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // URL
                        OutlinedTextField(
                            value = url,
                            onValueChange = {
                                url = it
                                errorMessage = null
                            },
                            label = { Text("Server URL") },
                            placeholder = { Text("e.g., http://localhost:3000/sse") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Help text
                        Text(
                            text = "Enter the SSE endpoint URL",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Error message
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            when {
                                transportType == "stdio" && command.isBlank() -> errorMessage = "Command is required"
                                transportType == "sse" && url.isBlank() -> errorMessage = "URL is required"
                                else -> {
                                    val config = if (transportType == "stdio") {
                                        val args = argsText.lines()
                                            .map { it.trim() }
                                            .filter { it.isNotBlank() }

                                        initialConfig.copy(
                                            command = command.trim(),
                                            url = null,
                                            args = args
                                        )
                                    } else {
                                        initialConfig.copy(
                                            command = null,
                                            args = emptyList(),
                                            url = url.trim()
                                        )
                                    }
                                    onSave(config)
                                }
                            }
                        },
                        enabled = (transportType == "stdio" && command.isNotBlank()) ||
                                  (transportType == "sse" && url.isNotBlank())
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                }
            }
        }
    }
}

/**
 * Dialog for confirming server deletion
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteConfirmationDialog(
    serverName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text("Delete MCP Server?")
        },
        text = {
            Text("Are you sure you want to delete the server \"$serverName\"? This action cannot be undone.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

