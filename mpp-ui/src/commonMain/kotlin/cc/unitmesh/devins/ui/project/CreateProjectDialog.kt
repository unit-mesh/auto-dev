package cc.unitmesh.devins.ui.project

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
import kotlinx.coroutines.launch

/**
 * CreateProjectDialog - 创建项目对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectDialog(
    viewModel: ProjectViewModel,
    onDismiss: () -> Unit
) {
    var projectName by remember { mutableStateOf("") }
    var projectDescription by remember { mutableStateOf("") }
    var useGit by remember { mutableStateOf(false) }
    var gitUrl by remember { mutableStateOf("") }
    var gitBranch by remember { mutableStateOf("main") }
    var gitUsername by remember { mutableStateOf("") }
    var gitPassword by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
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
                .fillMaxHeight(0.8f),
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
                        text = "创建新项目",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Project name
                    OutlinedTextField(
                        value = projectName,
                        onValueChange = { projectName = it },
                        label = { Text("项目名称") },
                        placeholder = { Text("my-awesome-project") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // Project description
                    OutlinedTextField(
                        value = projectDescription,
                        onValueChange = { projectDescription = it },
                        label = { Text("项目描述（可选）") },
                        placeholder = { Text("描述你的项目...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                    
                    HorizontalDivider()
                    
                    // Git configuration
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = useGit,
                            onCheckedChange = { useGit = it }
                        )
                        Text(
                            text = "从 Git 仓库创建",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    
                    if (useGit) {
                        OutlinedTextField(
                            value = gitUrl,
                            onValueChange = { gitUrl = it },
                            label = { Text("Git URL") },
                            placeholder = { Text("https://github.com/user/repo.git") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = gitBranch,
                            onValueChange = { gitBranch = it },
                            label = { Text("分支") },
                            placeholder = { Text("main") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = gitUsername,
                                onValueChange = { gitUsername = it },
                                label = { Text("用户名（可选）") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = gitPassword,
                                onValueChange = { gitPassword = it },
                                label = { Text("密码/Token（可选）") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                    
                    // Error message
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
                
                // Footer buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isCreating
                    ) {
                        Text("取消")
                    }
                    
                    Spacer(Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    isCreating = true
                                    errorMessage = null
                                    
                                    if (projectName.isBlank()) {
                                        errorMessage = "请输入项目名称"
                                        return@launch
                                    }
                                    
                                    if (useGit && gitUrl.isBlank()) {
                                        errorMessage = "请输入 Git URL"
                                        return@launch
                                    }
                                    
                                    val request = CreateProjectRequest(
                                        name = projectName,
                                        description = projectDescription.takeIf { it.isNotBlank() },
                                        gitUrl = if (useGit) gitUrl else null,
                                        gitBranch = if (useGit) gitBranch.takeIf { it.isNotBlank() } else null,
                                        gitUsername = if (useGit) gitUsername.takeIf { it.isNotBlank() } else null,
                                        gitPassword = if (useGit) gitPassword.takeIf { it.isNotBlank() } else null
                                    )
                                    
                                    val project = viewModel.createProject(request)
                                    if (project != null) {
                                        onDismiss()
                                    } else {
                                        errorMessage = "创建项目失败"
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "错误: ${e.message}"
                                } finally {
                                    isCreating = false
                                }
                            }
                        },
                        enabled = !isCreating && projectName.isNotBlank()
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (isCreating) "创建中..." else "创建")
                    }
                }
            }
        }
    }
}

