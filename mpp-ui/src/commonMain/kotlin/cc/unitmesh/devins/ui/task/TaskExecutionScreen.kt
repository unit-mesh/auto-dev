package cc.unitmesh.devins.ui.task

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cc.unitmesh.agent.RemoteAgentEvent
import kotlinx.coroutines.launch

/**
 * TaskExecutionScreen - 任务执行界面
 * 显示 AI Agent 的实时执行过程
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskExecutionScreen(
    viewModel: TaskViewModel,
    onBack: () -> Unit
) {
    val currentTask by viewModel.currentTask.collectAsState()
    val agentEvents by viewModel.agentEvents.collectAsState()
    val isExecuting by viewModel.isExecuting.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // 自动滚动到最新事件
    LaunchedEffect(agentEvents.size) {
        if (agentEvents.isNotEmpty()) {
            listState.animateScrollToItem(agentEvents.size - 1)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentTask?.task ?: "任务执行",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        if (isExecuting) {
                            Text(
                                text = "执行中...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearCurrentTask()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (isExecuting) {
                        IconButton(onClick = { viewModel.stopExecution() }) {
                            Icon(Icons.Default.Stop, contentDescription = "停止执行")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Execution info card
            currentTask?.let { task ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "状态",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isExecuting) "执行中" else "已完成",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isExecuting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = "事件数",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = agentEvents.size.toString(),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }
            }
            
            HorizontalDivider()
            
            // Event timeline
            if (agentEvents.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isExecuting) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "正在启动 AI Agent...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "等待执行...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(agentEvents) { event ->
                        AgentEventItem(event)
                    }
                }
            }
        }
    }
}

@Composable
fun AgentEventItem(event: RemoteAgentEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            val (icon, color, label, content) = getEventInfo(event)
            
            // Event type badge
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = color,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "$icon $label",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
            
            if (content.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun getEventInfo(event: RemoteAgentEvent): Quadruple<String, Color, String, String> {
    return when (event) {
        is RemoteAgentEvent.CloneProgress -> {
            val content = if (event.progress != null) {
                "${event.stage} (${event.progress}%)"
            } else {
                event.stage
            }
            Quadruple("📦", Color(0xFF9C27B0), "克隆进度", content)
        }
        is RemoteAgentEvent.CloneLog -> {
            Quadruple(
                if (event.isError) "❌" else "📝",
                if (event.isError) Color(0xFFF44336) else Color(0xFF9C27B0),
                "克隆日志",
                event.message
            )
        }
        is RemoteAgentEvent.Iteration -> {
            Quadruple("🔄", Color(0xFF2196F3), "迭代", "第 ${event.current}/${event.max} 次迭代")
        }
        is RemoteAgentEvent.LLMChunk -> {
            Quadruple("💬", Color(0xFF4CAF50), "AI 思考", event.chunk)
        }
        is RemoteAgentEvent.ToolCall -> {
            Quadruple("🔧", Color(0xFFFF9800), "工具调用", "调用: ${event.toolName}")
        }
        is RemoteAgentEvent.ToolResult -> {
            val icon = if (event.success) "✅" else "❌"
            val color = if (event.success) Color(0xFF8BC34A) else Color(0xFFF44336)
            val content = event.output?.take(200) ?: "无输出"
            Quadruple(icon, color, "工具结果", content)
        }
        is RemoteAgentEvent.Error -> {
            Quadruple("❌", Color(0xFFF44336), "错误", event.message)
        }
        is RemoteAgentEvent.Complete -> {
            val icon = if (event.success) "🎉" else "❌"
            val color = if (event.success) Color(0xFF00BCD4) else Color(0xFFF44336)
            val content = buildString {
                append(event.message)
                append("\n完成 ${event.iterations} 次迭代")
                if (event.edits.isNotEmpty()) {
                    append("\n修改了 ${event.edits.size} 个文件")
                }
            }
            Quadruple(icon, color, if (event.success) "完成" else "失败", content)
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

