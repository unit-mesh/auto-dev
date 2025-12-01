package cc.unitmesh.devins.idea.toolwindow.remote

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.idea.toolwindow.timeline.IdeaTimelineContent
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*

/**
 * Remote Agent Content UI for IntelliJ IDEA plugin.
 *
 * Displays:
 * - Server configuration inputs (URL, project/git URL)
 * - Connection status indicator
 * - Timeline content from remote agent execution
 */
@Composable
fun IdeaRemoteAgentContent(
    viewModel: IdeaRemoteAgentViewModel,
    listState: LazyListState,
    onProjectIdChange: (String) -> Unit = {},
    onGitUrlChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val timeline by viewModel.renderer.timeline.collectAsState()
    val streamingOutput by viewModel.renderer.currentStreamingOutput.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val connectionError by viewModel.connectionError.collectAsState()
    val availableProjects by viewModel.availableProjects.collectAsState()

    var serverUrl by remember { mutableStateOf(viewModel.serverUrl) }
    var projectId by remember { mutableStateOf("") }
    var gitUrl by remember { mutableStateOf("") }

    // Check connection on initial load and when server URL changes
    LaunchedEffect(serverUrl) {
        if (serverUrl.isNotBlank()) {
            viewModel.updateServerUrl(serverUrl)
            viewModel.checkConnection()
        }
    }

    // Propagate changes to parent
    LaunchedEffect(projectId) {
        onProjectIdChange(projectId)
    }
    LaunchedEffect(gitUrl) {
        onGitUrlChange(gitUrl)
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Server Configuration Panel
        RemoteConfigPanel(
            serverUrl = serverUrl,
            onServerUrlChange = { serverUrl = it },
            projectId = projectId,
            onProjectIdChange = { projectId = it },
            gitUrl = gitUrl,
            onGitUrlChange = { gitUrl = it },
            isConnected = isConnected,
            connectionError = connectionError,
            availableProjects = availableProjects,
            onConnect = { viewModel.checkConnection() },
            modifier = Modifier.fillMaxWidth()
        )

        // Timeline Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            IdeaTimelineContent(
                timeline = timeline,
                streamingOutput = streamingOutput,
                listState = listState
            )
        }
    }
}

/**
 * Configuration panel for remote server settings.
 * Uses TextFieldState for Jewel TextField compatibility.
 */
@Composable
private fun RemoteConfigPanel(
    serverUrl: String,
    onServerUrlChange: (String) -> Unit,
    projectId: String,
    onProjectIdChange: (String) -> Unit,
    gitUrl: String,
    onGitUrlChange: (String) -> Unit,
    isConnected: Boolean,
    connectionError: String?,
    availableProjects: List<ProjectInfo>,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    // TextFieldState for Jewel TextField
    val serverUrlState = rememberTextFieldState(serverUrl)
    val projectIdState = rememberTextFieldState(projectId)
    val gitUrlState = rememberTextFieldState(gitUrl)

    // Sync server URL state to callback
    LaunchedEffect(Unit) {
        snapshotFlow { serverUrlState.text.toString() }
            .distinctUntilChanged()
            .collect { onServerUrlChange(it) }
    }

    // Sync project ID state to callback
    LaunchedEffect(Unit) {
        snapshotFlow { projectIdState.text.toString() }
            .distinctUntilChanged()
            .collect { onProjectIdChange(it) }
    }

    // Sync git URL state to callback
    LaunchedEffect(Unit) {
        snapshotFlow { gitUrlState.text.toString() }
            .distinctUntilChanged()
            .collect { onGitUrlChange(it) }
    }

    // Sync external changes to text field states
    LaunchedEffect(serverUrl) {
        if (serverUrlState.text.toString() != serverUrl) {
            serverUrlState.setTextAndPlaceCursorAtEnd(serverUrl)
        }
    }
    LaunchedEffect(projectId) {
        if (projectIdState.text.toString() != projectId) {
            projectIdState.setTextAndPlaceCursorAtEnd(projectId)
        }
    }
    LaunchedEffect(gitUrl) {
        if (gitUrlState.text.toString() != gitUrl) {
            gitUrlState.setTextAndPlaceCursorAtEnd(gitUrl)
        }
    }

    Column(
        modifier = modifier
            .background(JewelTheme.globalColors.panelBackground)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Server URL row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Server:",
                style = JewelTheme.defaultTextStyle,
                modifier = Modifier.width(60.dp)
            )

            TextField(
                state = serverUrlState,
                placeholder = { Text("http://localhost:8080") },
                modifier = Modifier.weight(1f)
            )

            DefaultButton(
                onClick = onConnect,
                modifier = Modifier.height(32.dp)
            ) {
                Text("Connect")
            }
        }

        // Connection Status
        ConnectionStatusBar(
            isConnected = isConnected,
            serverUrl = serverUrl,
            connectionError = connectionError,
            modifier = Modifier.fillMaxWidth()
        )

        // Project/Git URL inputs (only show when connected)
        if (isConnected) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Project:",
                    style = JewelTheme.defaultTextStyle,
                    modifier = Modifier.width(60.dp)
                )

                if (availableProjects.isNotEmpty()) {
                    Dropdown(
                        menuContent = {
                            availableProjects.forEach { project ->
                                selectableItem(
                                    selected = projectId == project.id,
                                    onClick = {
                                        onProjectIdChange(project.id)
                                        projectIdState.setTextAndPlaceCursorAtEnd(project.id)
                                    }
                                ) {
                                    Text(project.name)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = availableProjects.find { it.id == projectId }?.name ?: "Select project..."
                        )
                    }
                } else {
                    TextField(
                        state = projectIdState,
                        placeholder = { Text("Project ID or name") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Git URL input (optional)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Git URL:",
                    style = JewelTheme.defaultTextStyle,
                    modifier = Modifier.width(60.dp)
                )

                TextField(
                    state = gitUrlState,
                    placeholder = { Text("https://github.com/user/repo.git (optional)") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Connection status indicator
 */
@Composable
private fun ConnectionStatusBar(
    isConnected: Boolean,
    serverUrl: String,
    connectionError: String?,
    modifier: Modifier = Modifier
) {
    val statusColor by animateColorAsState(
        targetValue = if (isConnected) AutoDevColors.Green.c400 else AutoDevColors.Red.c400,
        label = "statusColor"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(JewelTheme.globalColors.panelBackground.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status indicator dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color = statusColor, shape = CircleShape)
        )

        Text(
            text = if (isConnected) {
                "Connected to $serverUrl"
            } else if (connectionError != null) {
                "Error: $connectionError"
            } else {
                "Not connected"
            },
            style = JewelTheme.defaultTextStyle.copy(
                color = JewelTheme.globalColors.text.normal.copy(alpha = 0.7f)
            )
        )
    }
}

/**
 * Get the project ID or Git URL for task execution.
 * Handles trailing slashes and empty segments in Git URLs.
 */
fun getEffectiveProjectId(projectId: String, gitUrl: String): String {
    return if (gitUrl.isNotBlank()) {
        gitUrl.trimEnd('/')
            .split('/')
            .lastOrNull { it.isNotBlank() }
            ?.removeSuffix(".git")
            ?.ifBlank { projectId }
            ?: projectId
    } else {
        projectId
    }
}

