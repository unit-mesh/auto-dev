package cc.unitmesh.devins.idea.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.agent.config.*
import cc.unitmesh.agent.mcp.McpServerConfig
import cc.unitmesh.devins.idea.services.IdeaToolConfigService
import cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons
import cc.unitmesh.devins.ui.config.ConfigManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.jewel.bridge.JewelComposePanel
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.component.Text
import java.awt.Dimension
import javax.swing.JComponent

// JSON serialization helpers
private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    isLenient = true
}

private fun serializeMcpConfig(servers: Map<String, McpServerConfig>): String {
    if (servers.isEmpty()) return getDefaultMcpConfigTemplate()
    return try {
        json.encodeToString(servers)
    } catch (e: Exception) {
        getDefaultMcpConfigTemplate()
    }
}

private fun deserializeMcpConfig(jsonString: String): Result<Map<String, McpServerConfig>> {
    if (jsonString.isBlank()) return Result.success(emptyMap())
    return try {
        val servers = json.decodeFromString<Map<String, McpServerConfig>>(jsonString)
        // Validate each server config
        servers.forEach { (name, config) ->
            if (!config.validate()) {
                return Result.failure(Exception("Invalid config for '$name': must have 'command' or 'url'"))
            }
        }
        Result.success(servers)
    } catch (e: Exception) {
        Result.failure(Exception("Failed to parse JSON: ${e.message}"))
    }
}

private fun getDefaultMcpConfigTemplate(): String = """
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

/**
 * DialogWrapper for MCP configuration that uses IntelliJ's native dialog system.
 * This ensures proper z-index handling and centers the dialog in the IDE window.
 */
class IdeaMcpConfigDialogWrapper(
    private val project: Project?
) : DialogWrapper(project) {

    init {
        title = "MCP Configuration"
        init()
        contentPanel.border = JBUI.Borders.empty()
        rootPane.border = JBUI.Borders.empty()
    }

    override fun createSouthPanel(): JComponent? = null

    override fun createCenterPanel(): JComponent {
        val dialogPanel = JewelComposePanel {
            IdeaMcpConfigDialogContent(
                project = project,
                onDismiss = { close(CANCEL_EXIT_CODE) }
            )
        }
        dialogPanel.preferredSize = Dimension(850, 650)
        return dialogPanel
    }

    companion object {
        /**
         * Show the MCP configuration dialog.
         * @return true if the dialog was closed with OK, false otherwise
         */
        fun show(project: Project?): Boolean {
            val dialog = IdeaMcpConfigDialogWrapper(project)
            return dialog.showAndGet()
        }
    }
}

/**
 * Content for the MCP configuration dialog.
 * Extracted to be used both in Compose Dialog and DialogWrapper.
 */
@Composable
fun IdeaMcpConfigDialogContent(
    project: Project?,
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

    // Get tool config service for notifying state changes
    val toolConfigService = remember(project) {
        project?.let { IdeaToolConfigService.getInstance(it) }
    }

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

                    // Use service to save and notify listeners
                    if (toolConfigService != null) {
                        toolConfigService.saveAndUpdateConfig(updatedConfig)
                    } else {
                        ConfigManager.saveToolConfig(updatedConfig)
                    }
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
            .width(850.dp)
            .heightIn(max = 650.dp)
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
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            // Title row with auto-save indicator
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
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (hasUnsavedChanges) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(JewelTheme.globalColors.borders.normal.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = IdeaComposeIcons.History,
                                    contentDescription = "Auto-saving",
                                    modifier = Modifier.size(14.dp),
                                    tint = JewelTheme.globalColors.text.info
                                )
                                Text(
                                    text = "Auto-saving...",
                                    style = JewelTheme.defaultTextStyle.copy(
                                        fontSize = 11.sp,
                                        color = JewelTheme.globalColors.text.info
                                    )
                                )
                            }
                        }
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = IdeaComposeIcons.Close,
                        contentDescription = "Close"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading configuration...")
                }
            } else {
                // Tab row - styled like Material TabRow
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(JewelTheme.globalColors.borders.normal.copy(alpha = 0.1f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    McpTabButton(
                        text = "Tools",
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    McpTabButton(
                        text = "MCP Servers",
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Error message with styled container
                mcpLoadError?.let { error ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFEBEE))
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = IdeaComposeIcons.Error,
                                contentDescription = "Error",
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = error,
                                style = JewelTheme.defaultTextStyle.copy(
                                    fontSize = 12.sp,
                                    color = Color(0xFFD32F2F)
                                )
                            )
                        }
                    }
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

                Spacer(modifier = Modifier.height(12.dp))

                // Footer with summary and actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Summary column
                    Column(modifier = Modifier.weight(1f)) {
                        val enabledMcp = mcpTools.values.flatten().count { it.enabled }
                        val totalMcp = mcpTools.values.flatten().size
                        Text(
                            text = "MCP Tools: $enabledMcp/$totalMcp enabled | Built-in tools: Always enabled",
                            style = JewelTheme.defaultTextStyle.copy(
                                fontSize = 12.sp,
                                color = JewelTheme.globalColors.text.normal.copy(alpha = 0.7f)
                            )
                        )
                        if (hasUnsavedChanges) {
                            Text(
                                text = "Changes will be auto-saved in 2 seconds...",
                                style = JewelTheme.defaultTextStyle.copy(
                                    fontSize = 11.sp,
                                    color = JewelTheme.globalColors.text.info
                                )
                            )
                        } else {
                            Text(
                                text = "All changes saved",
                                style = JewelTheme.defaultTextStyle.copy(
                                    fontSize = 11.sp,
                                    color = JewelTheme.globalColors.text.normal.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        DefaultButton(onClick = onDismiss) {
                            Text("Apply & Close")
                        }
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (selected) JewelTheme.globalColors.panelBackground
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = JewelTheme.defaultTextStyle.copy(
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                color = if (selected) JewelTheme.globalColors.text.normal
                else JewelTheme.globalColors.text.normal.copy(alpha = 0.6f)
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
    val expandedServers = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Info banner about built-in tools
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(JewelTheme.globalColors.borders.normal.copy(alpha = 0.1f))
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = IdeaComposeIcons.Info,
                        contentDescription = "Info",
                        tint = JewelTheme.globalColors.text.info,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Built-in Tools Always Enabled",
                            style = JewelTheme.defaultTextStyle.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = JewelTheme.globalColors.text.info
                            )
                        )
                        Text(
                            text = "File operations, search, shell, and other essential tools are always available",
                            style = JewelTheme.defaultTextStyle.copy(
                                fontSize = 12.sp,
                                color = JewelTheme.globalColors.text.normal.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
            }
        }

        // MCP servers with tools
        mcpTools.forEach { (serverName, tools) ->
            val isExpanded = expandedServers.getOrPut(serverName) { true }
            val serverState = mcpLoadingState.servers[serverName]

            item(key = "server_$serverName") {
                McpServerHeader(
                    serverName = serverName,
                    serverState = serverState,
                    tools = tools,
                    isExpanded = isExpanded,
                    onToggle = { expandedServers[serverName] = !isExpanded }
                )
            }

            if (isExpanded) {
                items(tools, key = { "tool_${it.name}" }) { tool ->
                    CompactToolItemRow(
                        tool = tool,
                        onToggle = { enabled -> onToolToggle(tool.name, enabled) }
                    )
                }
            }
        }

        val isLoading = mcpLoadingState.loadingServers.isNotEmpty()

        if (mcpTools.isEmpty() && !isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No MCP tools configured. Add MCP servers in the 'MCP Servers' tab.",
                        style = JewelTheme.defaultTextStyle.copy(
                            color = JewelTheme.globalColors.text.normal.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        }

        if (isLoading) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Loading MCP tools...")
                }
            }
        }
    }
}

@Composable
private fun McpServerHeader(
    serverName: String,
    serverState: McpServerState?,
    tools: List<ToolItem>,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(JewelTheme.globalColors.borders.normal.copy(alpha = 0.15f))
            .clickable(onClick = onToggle)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon
            val (statusIcon, statusColor) = when (serverState?.status) {
                McpServerLoadingStatus.LOADING -> IdeaComposeIcons.Refresh to JewelTheme.globalColors.text.info
                McpServerLoadingStatus.LOADED -> IdeaComposeIcons.Cloud to Color(0xFF4CAF50)
                McpServerLoadingStatus.ERROR -> IdeaComposeIcons.Error to Color(0xFFD32F2F)
                else -> IdeaComposeIcons.Cloud to JewelTheme.globalColors.text.normal.copy(alpha = 0.5f)
            }

            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "MCP: $serverName",
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.weight(1f)
            )

            if (tools.isNotEmpty()) {
                Text(
                    text = "${tools.count { it.enabled }}/${tools.size}",
                    style = JewelTheme.defaultTextStyle.copy(
                        fontSize = 12.sp,
                        color = JewelTheme.globalColors.text.info
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (serverState?.isLoading == true) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }

            Icon(
                imageVector = if (isExpanded) IdeaComposeIcons.ExpandLess else IdeaComposeIcons.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = JewelTheme.globalColors.text.normal.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (isChecked) JewelTheme.globalColors.borders.normal.copy(alpha = 0.08f)
                else Color.Transparent
            )
            .clickable {
                isChecked = !isChecked
                onToggle(isChecked)
            }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = {
                isChecked = it
                onToggle(it)
            }
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Tool name
        Text(
            text = tool.displayName,
            style = JewelTheme.defaultTextStyle.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.width(140.dp),
            maxLines = 1
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Description
        Text(
            text = tool.description,
            style = JewelTheme.defaultTextStyle.copy(
                fontSize = 12.sp,
                color = JewelTheme.globalColors.text.normal.copy(alpha = 0.6f)
            ),
            modifier = Modifier.weight(1f),
            maxLines = 1
        )

        // Server badge
        if (tool.serverName.isNotEmpty()) {
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .background(JewelTheme.globalColors.borders.normal.copy(alpha = 0.2f))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = tool.serverName,
                    style = JewelTheme.defaultTextStyle.copy(fontSize = 9.sp)
                )
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
        // Header with title and validation status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MCP Server Configuration",
                    style = JewelTheme.defaultTextStyle.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = "JSON is validated in real-time",
                    style = JewelTheme.defaultTextStyle.copy(
                        fontSize = 11.sp,
                        color = JewelTheme.globalColors.text.normal.copy(alpha = 0.6f)
                    )
                )
            }

            // Validation status indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isReloading) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp))
                    Text(
                        text = "Loading...",
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 11.sp,
                            color = JewelTheme.globalColors.text.info
                        )
                    )
                } else if (errorMessage != null) {
                    Icon(
                        imageVector = IdeaComposeIcons.Error,
                        contentDescription = "Error",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Invalid JSON",
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 11.sp,
                            color = Color(0xFFD32F2F)
                        )
                    )
                } else if (mcpConfigJson.isNotBlank()) {
                    Icon(
                        imageVector = IdeaComposeIcons.CheckCircle,
                        contentDescription = "Valid",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Valid JSON",
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 11.sp,
                            color = Color(0xFF4CAF50)
                        )
                    )
                }
            }
        }

        // Error message detail
        errorMessage?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFFFEBEE))
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = IdeaComposeIcons.Error,
                        contentDescription = "Error",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = error,
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 11.sp,
                            color = Color(0xFFD32F2F)
                        )
                    )
                }
            }
        }

        // JSON editor with border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .border(
                    width = 1.dp,
                    color = if (errorMessage != null) Color(0xFFD32F2F)
                    else JewelTheme.globalColors.borders.normal,
                    shape = RoundedCornerShape(6.dp)
                )
                .background(JewelTheme.globalColors.panelBackground)
                .padding(8.dp)
        ) {
            BasicTextField(
                state = textFieldState,
                modifier = Modifier.fillMaxSize(),
                textStyle = TextStyle(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = JewelTheme.globalColors.text.normal
                ),
                cursorBrush = SolidColor(JewelTheme.globalColors.text.normal)
            )
        }

        // Footer with hint and reload button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Example: uvx for Python tools, npx for Node.js tools",
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 11.sp,
                    color = JewelTheme.globalColors.text.normal.copy(alpha = 0.6f)
                )
            )

            DefaultButton(
                onClick = onReload,
                enabled = !isReloading && errorMessage == null
            ) {
                if (isReloading) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                } else {
                    Icon(
                        imageVector = IdeaComposeIcons.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(if (isReloading) "Loading..." else "Save & Reload")
            }
        }
    }
}
