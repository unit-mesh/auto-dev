package cc.unitmesh.devins.ui.task

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
import kotlinx.coroutines.launch

/**
 * CreateTaskDialog - 创建任务对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskDialog(
    viewModel: TaskViewModel,
    onDismiss: () -> Unit
) {
    var taskDescription by remember { mutableStateOf("") }
    var maxIterations by remember { mutableStateOf(20) }
    
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
                .fillMaxHeight(0.7f),
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
                        text = "创建新任务",
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
                    // Task description
                    Text(
                        text = "任务描述",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Text(
                        text = "使用 / 命令，@ 提及 agent。Ctrl+P 增强提示。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
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
                            initialText = taskDescription,
                            placeholder = "描述你希望 AI Agent 完成的任务...\n\n示例：\n/write 实现用户认证功能\n@architect 设计数据库架构",
                            callbacks = object : EditorCallbacks {
                                override fun onSubmit(text: String) {
                                    taskDescription = text
                                }
                                
                                override fun onTextChanged(text: String) {
                                    taskDescription = text
                                }
                            },
                            isCompactMode = false,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    HorizontalDivider()
                    
                    // Advanced settings
                    Text(
                        text = "高级设置",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "最大迭代次数:",
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
                                    
                                    if (taskDescription.isBlank()) {
                                        errorMessage = "请描述任务"
                                        return@launch
                                    }
                                    
                                    val task = viewModel.createTask(
                                        taskDescription = taskDescription,
                                        maxIterations = maxIterations
                                    )
                                    
                                    if (task != null) {
                                        // 创建成功后自动执行
                                        viewModel.executeTask(task)
                                        onDismiss()
                                    } else {
                                        errorMessage = "创建任务失败"
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "错误: ${e.message}"
                                } finally {
                                    isCreating = false
                                }
                            }
                        },
                        enabled = !isCreating && taskDescription.isNotBlank()
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (isCreating) "创建中..." else "创建并执行")
                    }
                }
            }
        }
    }
}

