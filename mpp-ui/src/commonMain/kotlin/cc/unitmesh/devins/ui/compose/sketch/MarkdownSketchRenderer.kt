package cc.unitmesh.devins.ui.compose.sketch

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.parser.CodeFence
import com.mikepenz.markdown.m3.Markdown

/**
 * Markdown Sketch 渲染器 - 使用 multiplatform-markdown-renderer 渲染 Markdown 内容
 * 参考 SketchRenderer 的设计，但使用真正的 Markdown 渲染器
 * 
 * 特性：
 * - 完整的 Markdown 渲染支持（标题、列表、粗体、斜体、链接等）
 * - 代码块语法高亮
 * - Diff 块特殊渲染
 * - 流式渲染支持（适合 LLM 响应）
 */
object MarkdownSketchRenderer {
    
    /**
     * 渲染 LLM 响应内容
     * 使用 CodeFence.parseAll() 解析内容，然后根据类型渲染
     * - Markdown 文本：使用 multiplatform-markdown-renderer 渲染
     * - 代码块：使用 Material Card 渲染
     * - Diff 块：使用 DiffSketchRenderer 渲染
     */
    @Composable
    fun RenderResponse(
        content: String,
        isComplete: Boolean = false,
        modifier: Modifier = Modifier
    ) {
        val scrollState = rememberScrollState()
        
        // 自动滚动到底部
        LaunchedEffect(content) {
            if (content.isNotEmpty()) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }
        
        Column(
            modifier = modifier
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // 使用 CodeFence 解析内容
            val codeFences = CodeFence.parseAll(content)
            
            codeFences.forEach { fence ->
                when (fence.languageId.lowercase()) {
                    "markdown", "md", "" -> {
                        // Markdown 文本块 - 使用 multiplatform-markdown-renderer
                        if (fence.text.isNotBlank()) {
                            MarkdownTextRenderer(fence.text)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                    "diff", "patch" -> {
                        // Diff 块 - 使用专门的 DiffSketchRenderer
                        DiffSketchRenderer.RenderDiff(
                            diffContent = fence.text,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    else -> {
                        // 代码块 - 使用 Card 渲染
                        CodeBlockRenderer(
                            code = fence.text,
                            language = fence.languageId,
                            displayName = CodeFence.displayNameByExt(fence.extension ?: fence.languageId)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
            
            // 如果未完成，显示加载指示器
            if (!isComplete && content.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
    
    /**
     * 渲染 Markdown 文本
     * 使用 multiplatform-markdown-renderer 的 Markdown 组件
     */
    @Composable
    private fun MarkdownTextRenderer(markdown: String) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            // 使用 multiplatform-markdown-renderer 渲染 Markdown
            Markdown(
                content = markdown,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
    
    /**
     * 渲染代码块
     * 使用 Material Card 包装，显示语言标签和代码内容
     */
    @Composable
    private fun CodeBlockRenderer(code: String, language: String, displayName: String = language) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 语言标签
                if (displayName.isNotEmpty() && displayName != "markdown") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        // 可以添加复制按钮
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
                }
                
                // 将代码内容用 Markdown 代码块格式包装，让 Markdown 渲染器处理
                val codeBlock = if (displayName.isNotEmpty() && displayName != "markdown") {
                    "```$language\n$code\n```"
                } else {
                    "```\n$code\n```"
                }
                
                Markdown(
                    content = codeBlock,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
    
    /**
     * 纯文本渲染（不解析 Markdown）
     * 用于已经渲染过的内容或需要保持原始格式的内容
     */
    @Composable
    fun RenderPlainText(
        text: String,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
    
    /**
     * 渲染单个 Markdown 内容块（不使用 CodeFence 解析）
     * 适合已知内容是纯 Markdown 的场景
     */
    @Composable
    fun RenderMarkdown(
        markdown: String,
        modifier: Modifier = Modifier
    ) {
        Markdown(
            content = markdown,
            modifier = modifier.padding(16.dp)
        )
    }
}

