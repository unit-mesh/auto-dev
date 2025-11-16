package cc.unitmesh.devins.ui.compose.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.llm.Message
import cc.unitmesh.devins.llm.MessageRole
import cc.unitmesh.devins.ui.compose.sketch.SketchRenderer

/**
 * 单条消息项 - 使用统一的连续流式布局
 */
@Composable
fun MessageItem(message: Message) {
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
                    modifier =
                        Modifier
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
fun StreamingMessageItem(
    content: String,
    onContentUpdate: (blockCount: Int) -> Unit = {}
) {
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

        Box(modifier = Modifier.fillMaxWidth().padding(start = 32.dp)) {
            SketchRenderer.RenderResponse(
                content = content,
                isComplete = false,
                onContentUpdate = onContentUpdate,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun MessageLabel(
    role: MessageRole,
    modifier: Modifier = Modifier
) {
    val (icon, label, color) =
        when (role) {
            MessageRole.USER -> Triple("👤", "You", MaterialTheme.colorScheme.secondary)
            MessageRole.ASSISTANT -> Triple("🤖", "AI Assistant", MaterialTheme.colorScheme.primary)
            MessageRole.SYSTEM -> Triple("⚙️", "System", MaterialTheme.colorScheme.tertiary)
        }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Companion.CenterVertically
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
