package cc.unitmesh.devins.ui.wasm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Wasm 平台的 Git 克隆界面
 * 允许用户输入 Git 仓库 URL 并克隆，显示操作日志和提交历史
 *
 * 使用全局共享的 GitOperations 实例（通过 WasmGitManager），
 * 避免重复初始化和目录丢失问题
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WasmGitCloneScreen(
    viewModel: WasmGitViewModel = remember {
        WasmGitViewModel(gitOperations = WasmGitManager.getInstance())
    },
    onClose: (() -> Unit)? = null,
    onCommitsFetched: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val logListState = rememberLazyListState()

    // 自动滚动到日志底部
    LaunchedEffect(uiState.logs.size) {
        if (uiState.logs.isNotEmpty()) {
            logListState.animateScrollToItem(uiState.logs.size - 1)
        }
    }

    // 当成功获取 commits 后，触发回调返回主界面
    LaunchedEffect(uiState.commits.size) {
        if (uiState.commits.isNotEmpty() && uiState.cloneSuccess) {
            onCommitsFetched?.invoke()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Git Repository Clone (WebAssembly) by https://cors-anywhere.com/") },
                actions = {
                    if (onClose != null) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Clone Git Repository",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = uiState.repoUrl,
                        onValueChange = { viewModel.updateRepoUrl(it) },
                        label = { Text("Repository URL") },
                        placeholder = { Text("https://github.com/phodal-archive/mini-file") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading,
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = uiState.targetDir,
                        onValueChange = { viewModel.updateTargetDir(it) },
                        label = { Text("Target Directory (optional)") },
                        placeholder = { Text("Leave empty for auto-naming") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading,
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.cloneRepository()
                                }
                            },
                            enabled = !uiState.isLoading && uiState.repoUrl.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("Clone Repository")
                        }

                        if (uiState.cloneSuccess) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        viewModel.fetchCommitHistory()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Fetch Commits")
                            }
                        }
                    }

                    if (uiState.errorMessage != null) {
                        Text(
                            text = "Error: ${uiState.errorMessage}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (uiState.cloneSuccess) {
                        Text(
                            text = "✓ Repository cloned successfully!",
                            color = Color(0xFF4CAF50),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Logs section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Console Output",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (uiState.logs.isNotEmpty()) {
                            TextButton(onClick = { viewModel.clearLogs() }) {
                                Text("Clear", fontSize = 12.sp)
                            }
                        }
                    }

                    LazyColumn(
                        state = logListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1E1E1E))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (uiState.logs.isEmpty()) {
                            item {
                                Text(
                                    text = "No logs yet. Clone a repository to see output.",
                                    color = Color(0xFF808080),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            items(uiState.logs) { log ->
                                LogItem(log)
                            }
                        }
                    }
                }
            }

            // Commit history section
            if (uiState.commits.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Commit History (${uiState.commits.size} commits)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.commits) { commit ->
                                CommitItem(commit)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogItem(log: GitLog) {
    val color = when (log.type) {
        LogType.ERROR -> Color(0xFFF44336)
        LogType.WARNING -> Color(0xFFFF9800)
        LogType.SUCCESS -> Color(0xFF4CAF50)
        LogType.INFO -> Color(0xFF2196F3)
        LogType.DEBUG -> Color(0xFF9E9E9E)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = when (log.type) {
                LogType.ERROR -> "✗"
                LogType.WARNING -> "⚠"
                LogType.SUCCESS -> "✓"
                LogType.INFO -> "ℹ"
                LogType.DEBUG -> "→"
            },
            color = color,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp
        )
        Text(
            text = log.message,
            color = Color(0xFFCCCCCC),
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CommitItem(commit: GitCommitDisplay) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = commit.hash.take(7),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp
                )
                Text(
                    text = commit.dateFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            Text(
                text = commit.message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "${commit.author} <${commit.email}>",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

