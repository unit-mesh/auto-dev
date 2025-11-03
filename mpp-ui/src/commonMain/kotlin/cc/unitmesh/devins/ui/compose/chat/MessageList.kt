package cc.unitmesh.devins.ui.compose.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.filesystem.ProjectFileSystem
import cc.unitmesh.devins.llm.Message
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 消息列表组件
 * 显示完整的对话历史，使用连续流式布局
 * 
 * 优化的滚动策略：
 * 1. 检测用户是否手动滚动
 * 2. 流式输出时持续滚动到底部（除非用户主动向上滚动）
 * 3. 新消息到达时自动滚动
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
    val coroutineScope = rememberCoroutineScope()
    
    // 跟踪用户是否主动向上滚动
    var userScrolledAway by remember { mutableStateOf(false) }
    
    // 使用 derivedStateOf 来减少重组，只在真正需要时才触发
    val shouldAutoScroll by remember {
        derivedStateOf {
            isLLMProcessing && !userScrolledAway && currentOutput.isNotEmpty()
        }
    }
    
    // 滚动到底部的辅助函数
    fun scrollToBottomIfNeeded() {
        if (shouldAutoScroll) {
            coroutineScope.launch {
                val lastIndex = messages.size
                listState.scrollToItem(lastIndex)
            }
        }
    }
    
    // 监听滚动状态，检测用户是否手动滚动
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            // 用户正在滚动
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            
            // 如果用户滚动到的位置不是底部附近（倒数第2项以内），认为用户想查看历史
            userScrolledAway = lastVisibleIndex < totalItems - 2
        }
    }
    
    // 新消息到达时自动滚动（基于消息 ID 变化）
    LaunchedEffect(messages.lastOrNull()?.timestamp) {
        if (messages.isNotEmpty() && !isLLMProcessing) {
            // 新消息完成时，重置用户滚动状态并滚动到底部
            userScrolledAway = false
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    // 监听内容变化（每50字符或每新行）
    LaunchedEffect(currentOutput) {
        if (shouldAutoScroll) {
            val lineCount = currentOutput.count { it == '\n' }
            val chunkIndex = currentOutput.length / 100  // 改为每100字符，减少频率
            val contentSignature = lineCount + chunkIndex
            
            // 延迟执行，避免在布局完成前滚动
            delay(100)
            scrollToBottomIfNeeded()
        }
    }
    
    // 流式输出开始时，重置状态
    LaunchedEffect(isLLMProcessing) {
        if (isLLMProcessing) {
            userScrolledAway = false
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
                    StreamingMessageItem(
                        content = currentOutput,
                        onContentUpdate = { blockCount ->
                            // 块数量变化时触发滚动
                            scrollToBottomIfNeeded()
                        }
                    )
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

