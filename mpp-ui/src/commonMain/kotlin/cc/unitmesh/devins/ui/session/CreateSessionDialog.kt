package cc.unitmesh.devins.ui.session

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cc.unitmesh.devins.editor.EditorCallbacks
import cc.unitmesh.devins.ui.compose.editor.DevInEditorInput
import cc.unitmesh.devins.workspace.WorkspaceManager
import cc.unitmesh.session.SessionMetadata
import kotlinx.coroutines.launch

/**
 * 创建会话对话框
 *
 * 功能：
 * 1. 选择已有项目 OR 输入 Git URL
 * 2. 使用 DevInEditorInput 输入任务需求
 * 3. 配置 LLM 设置（可选）
 * 4. 创建会话并自动启动 Agent 执行
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSessionDialog(
    viewModel: SessionViewModel,
    onDismiss: () -> Unit
) {
    var projectSource by remember { mutableStateOf(ProjectSource.EXISTING) }
    var selectedProject by remember { mutableStateOf<String?>(null) }
    var gitUrl by remember { mutableStateOf("") }
    var gitBranch by remember { mutableStateOf("main") }
    var gitUsername by remember { mutableStateOf("") }
    var gitPassword by remember { mutableStateOf("") }
    var taskRequirement by remember { mutableStateOf("") }
    var maxIterations by remember { mutableStateOf(20) }

    val scope = rememberCoroutineScope()
    val currentWorkspace by WorkspaceManager.workspaceFlow.collectAsState()
    val availableProjects = remember(currentWorkspace) {
        listOf(currentWorkspace?.rootPath ?: "No workspace open")
    }

    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Create New Session",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Project Source Selection
                    Text(
                        text = "1. Select Project Source",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = projectSource == ProjectSource.EXISTING,
                            onClick = { projectSource = ProjectSource.EXISTING },
                            label = { Text("Existing Project") }
                        )
                        FilterChip(
                            selected = projectSource == ProjectSource.GIT,
                            onClick = { projectSource = ProjectSource.GIT },
                            label = { Text("Git Repository") }
                        )
                    }

                    // Project Source Input
                    when (projectSource) {
                        ProjectSource.EXISTING -> {
                            if (availableProjects.isNotEmpty()) {
                                ExposedDropdownMenuBox(
                                    expanded = false,
                                    onExpandedChange = { }
                                ) {
                                    OutlinedTextField(
                                        value = selectedProject ?: availableProjects.first(),
                                        onValueChange = { },
                                        label = { Text("Project Path") },
                                        readOnly = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor()
                                    )
                                }
                                selectedProject = selectedProject ?: availableProjects.first()
                            } else {
                                Text(
                                    text = "No workspace open. Please open a workspace first or use Git Repository.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        ProjectSource.GIT -> {
                            OutlinedTextField(
                                value = gitUrl,
                                onValueChange = { gitUrl = it },
                                label = { Text("Git URL") },
                                placeholder = { Text("https://github.com/user/repo.git") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = gitBranch,
                                    onValueChange = { gitBranch = it },
                                    label = { Text("Branch") },
                                    placeholder = { Text("main") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = gitUsername,
                                    onValueChange = { gitUsername = it },
                                    label = { Text("Username (optional)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            OutlinedTextField(
                                value = gitPassword,
                                onValueChange = { gitPassword = it },
                                label = { Text("Password/Token (optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }

                    HorizontalDivider()

                    // 2. Task Requirement Input
                    Text(
                        text = "2. Describe Your Task",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Use / for commands, @ for agents. Ctrl+P to enhance your prompt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 使用 DevInEditorInput 组件
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        shape = MaterialTheme.shapes.medium,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline
                        )
                    ) {
                        DevInEditorInput(
                            initialText = taskRequirement,
                            placeholder = "Describe what you want the AI agent to do...\n\nExample:\n/write Implement user authentication\n@architect Design the database schema",
                            callbacks = object : EditorCallbacks {
                                override fun onSubmit(text: String) {
                                    taskRequirement = text
                                }

                                override fun onTextChanged(text: String) {
                                    taskRequirement = text
                                }
                            },
                            isCompactMode = false,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    HorizontalDivider()

                    // 3. Advanced Settings
                    Text(
                        text = "3. Advanced Settings (Optional)",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Max Iterations:",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(120.dp)
                        )
                        Slider(
                            value = maxIterations.toFloat(),
                            onValueChange = { maxIterations = it.toInt() },
                            valueRange = 5f..50f,
                            steps = 8,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = maxIterations.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(40.dp)
                        )
                    }

                    // Error Message
                    errorMessage?.let { error ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isCreating
                    ) {
                        Text("Cancel")
                    }

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    isCreating = true
                                    errorMessage = null

                                    // Validate input
                                    val projectId = when (projectSource) {
                                        ProjectSource.EXISTING -> {
                                            selectedProject ?: run {
                                                errorMessage = "Please select a project"
                                                return@launch
                                            }
                                        }
                                        ProjectSource.GIT -> {
                                            if (gitUrl.isBlank()) {
                                                errorMessage = "Please enter a Git URL"
                                                return@launch
                                            }
                                            gitUrl
                                        }
                                    }

                                    if (taskRequirement.isBlank()) {
                                        errorMessage = "Please describe your task"
                                        return@launch
                                    }

                                    // Create session
                                    val session = viewModel.createSession(
                                        projectId = projectId,
                                        task = taskRequirement,
                                        metadata = SessionMetadata(
                                            maxIterations = maxIterations
                                        )
                                    )

                                    if (session == null) {
                                        errorMessage = "Failed to create session"
                                        return@launch
                                    }

                                    // Start agent execution
                                    // Note: executeSession 会在服务端处理 Git clone（如果有 gitUrl）
                                    if (projectSource == ProjectSource.GIT) {
                                        viewModel.executeSessionWithGit(
                                            sessionId = session.id,
                                            gitUrl = gitUrl,
                                            branch = gitBranch.takeIf { it.isNotBlank() },
                                            username = gitUsername.takeIf { it.isNotBlank() },
                                            password = gitPassword.takeIf { it.isNotBlank() }
                                        )
                                    } else {
                                        viewModel.executeSession(session.id)
                                    }

                                    // Join the session to see real-time updates
                                    viewModel.joinSession(session.id)

                                    // Close dialog
                                    onDismiss()
                                } catch (e: Exception) {
                                    errorMessage = "Error: ${e.message}"
                                } finally {
                                    isCreating = false
                                }
                            }
                        },
                        enabled = !isCreating && taskRequirement.isNotBlank()
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (isCreating) "Creating..." else "Create & Start")
                    }
                }
            }
        }
    }
}

enum class ProjectSource {
    EXISTING,
    GIT
}

