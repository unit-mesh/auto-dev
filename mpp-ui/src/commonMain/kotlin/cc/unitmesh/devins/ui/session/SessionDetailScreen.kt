package cc.unitmesh.devins.ui.session

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cc.unitmesh.session.SessionEventEnvelope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * SessionDetailScreen - 会话详情界面
 * 显示会话的实时事件流
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    viewModel: SessionViewModel,
    onBack: () -> Unit
) {
    val currentSession by viewModel.currentSession.collectAsState()
    val sessionEvents by viewModel.sessionEvents.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // 自动滚动到最新事件
    LaunchedEffect(sessionEvents.size) {
        if (sessionEvents.isNotEmpty()) {
            listState.animateScrollToItem(sessionEvents.size - 1)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = currentSession?.task ?: "会话详情",
                            style = MaterialTheme.typography.titleMedium
                        )
                        currentSession?.let {
                            Text(
                                text = "Project: ${it.projectId}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.leaveSession()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
            // Session info card
            currentSession?.let { session ->
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
                            StatusBadge(session.status)
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
                                text = sessionEvents.size.toString(),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }
            }
            
            Divider()
            
            // Event timeline
            if (isLoading && sessionEvents.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (sessionEvents.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "等待事件...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sessionEvents) { envelope ->
                        EventTimelineItem(envelope)
                    }
                }
            }
        }
    }
}

@Composable
fun EventTimelineItem(envelope: SessionEventEnvelope) {
    val json = remember { Json { ignoreUnknownKeys = true } }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Event type badge
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (icon, color, label) = getEventInfo(envelope.eventType)
                
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
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = formatTimestamp(envelope.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Event data
            Text(
                text = formatEventData(envelope.eventType, envelope.eventData, json),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun getEventInfo(eventType: String): Triple<String, Color, String> {
    return when (eventType) {
        "iteration" -> Triple("🔄", Color(0xFF2196F3), "迭代")
        "llm_chunk" -> Triple("💬", Color(0xFF4CAF50), "LLM 响应")
        "tool_call" -> Triple("🔧", Color(0xFFFF9800), "工具调用")
        "tool_result" -> Triple("✅", Color(0xFF8BC34A), "工具结果")
        "clone_log" -> Triple("📥", Color(0xFF9C27B0), "克隆日志")
        "clone_progress" -> Triple("📊", Color(0xFF9C27B0), "克隆进度")
        "error" -> Triple("❌", Color(0xFFF44336), "错误")
        "complete" -> Triple("🎉", Color(0xFF00BCD4), "完成")
        else -> Triple("📌", Color(0xFF9E9E9E), eventType)
    }
}

private fun formatEventData(eventType: String, eventData: String, json: Json): String {
    return try {
        // 简单地提取关键信息
        when (eventType) {
            "llm_chunk" -> {
                val data = json.parseToJsonElement(eventData)
                data.toString().substringAfter("chunk\":\"").substringBefore("\"")
            }
            "tool_call" -> {
                val data = json.parseToJsonElement(eventData)
                "Tool: ${data.toString().substringAfter("toolName\":\"").substringBefore("\"")}"
            }
            "error" -> {
                val data = json.parseToJsonElement(eventData)
                data.toString().substringAfter("message\":\"").substringBefore("\"")
            }
            else -> eventData.take(200)
        }
    } catch (e: Exception) {
        eventData.take(200)
    }
}

