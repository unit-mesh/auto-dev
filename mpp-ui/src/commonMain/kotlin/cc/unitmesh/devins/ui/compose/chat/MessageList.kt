package cc.unitmesh.devins.ui.compose.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.filesystem.ProjectFileSystem
import cc.unitmesh.devins.llm.Message
import cc.unitmesh.devins.llm.MessageRole
import cc.unitmesh.devins.ui.compose.sketch.SketchRenderer

/**
 * 消息列表组件
 * 显示完整的对话历史，使用连续流式布局
 */
@Composable
fun MessageList(
    messages: List<Message>,
    isLLMProcessing: Boolean,
    currentOutput: String,
    projectPath: String?,
    fileSystem: ProjectFileSystem,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    // 自动滚动到底部
    LaunchedEffect(messages.size, currentOutput) {
        if (messages.isNotEmpty() || currentOutput.isNotEmpty()) {
            // 总是滚动到最后一项
            val targetIndex = if (isLLMProcessing && currentOutput.isNotEmpty()) {
                messages.size  // 流式输出项的索引
            } else {
                maxOf(0, messages.size - 1)
            }
            listState.animateScrollToItem(targetIndex)
        }
    }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // 显示历史消息（不包含正在生成的）
            items(
                items = messages,
                key = { it.timestamp }
            ) { message ->
                MessageItem(message = message)
            }
            
            // 显示正在生成的 AI 响应（只在流式输出时显示）
            if (isLLMProcessing && currentOutput.isNotEmpty()) {
                item(key = "streaming") {
                    StreamingMessageItem(content = currentOutput)
                }
            }
        }
        
        // 底部项目信息
        ProjectInfoFooter(
            projectPath = projectPath,
            fileSystem = fileSystem,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 8.dp)
        )
    }
}

/**
 * 单条消息项 - 使用统一的连续流式布局
 */
@Composable
private fun MessageItem(message: Message) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 消息标签
        MessageLabel(
            role = message.role,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        
        // 消息内容 - 统一使用 SketchRenderer
        when (message.role) {
            MessageRole.SYSTEM -> {
                // 系统消息使用简单样式
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 32.dp, bottom = 8.dp)
                )
            }
            else -> {
                // 用户和 AI 消息都使用 SketchRenderer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp)
                ) {
                    SketchRenderer.RenderResponse(
                        content = message.content,
                        isComplete = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * 流式输出消息项
 */
@Composable
private fun StreamingMessageItem(content: String) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // AI 标签（带加载指示）
        Row(
            modifier = Modifier.padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🤖",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "AI Assistant",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 2.dp
            )
        }
        
        // 流式输出内容
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp)
        ) {
            SketchRenderer.RenderResponse(
                content = content,
                isComplete = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 消息标签
 */
@Composable
private fun MessageLabel(
    role: MessageRole,
    modifier: Modifier = Modifier
) {
    val (icon, label, color) = when (role) {
        MessageRole.USER -> Triple("👤", "You", MaterialTheme.colorScheme.secondary)
        MessageRole.ASSISTANT -> Triple("🤖", "AI Assistant", MaterialTheme.colorScheme.primary)
        MessageRole.SYSTEM -> Triple("⚙️", "System", MaterialTheme.colorScheme.tertiary)
    }
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = color
        )
    }
}

/**
 * 项目信息底部栏
 */
@Composable
private fun ProjectInfoFooter(
    projectPath: String?,
    fileSystem: ProjectFileSystem,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (projectPath != null) "📁 $projectPath" else "⚠️ No project selected",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        
        if (projectPath != null) {
            val commandCount = remember(fileSystem) {
                try {
                    cc.unitmesh.devins.command.SpecKitCommand.loadAll(fileSystem).size
                } catch (e: Exception) {
                    0
                }
            }
            
            Text(
                text = "✨ $commandCount SpecKit commands",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        }
    }
}

