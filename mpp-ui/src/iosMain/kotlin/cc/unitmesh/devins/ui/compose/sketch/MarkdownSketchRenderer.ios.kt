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
 * iOS 平台的 Markdown Sketch 渲染器实现
 * 使用 Compose + multiplatform-markdown-renderer
 */
actual object MarkdownSketchRenderer {
    @Composable
    actual fun RenderResponse(
        content: String,
        isComplete: Boolean,
        isDarkTheme: Boolean,
        modifier: Modifier
    ) {
        val scrollState = rememberScrollState()

        // 自动滚动到底部
        LaunchedEffect(content) {
            if (content.isNotEmpty()) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }

        Column(
            modifier =
                modifier
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
            colors =
                CardDefaults.cardColors(
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
    private fun CodeBlockRenderer(
        code: String,
        language: String,
        displayName: String = language
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
            ) {
                // 语言标签
                if (displayName.isNotEmpty() && displayName != "markdown") {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
                }

                // 将代码内容用 Markdown 代码块格式包装，让 Markdown 渲染器处理
                val codeBlock =
                    if (displayName.isNotEmpty() && displayName != "markdown") {
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

    @Composable
    actual fun RenderPlainText(
        text: String,
        modifier: Modifier
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors =
                CardDefaults.cardColors(
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

    @Composable
    actual fun RenderMarkdown(
        markdown: String,
        isDarkTheme: Boolean,
        modifier: Modifier
    ) {
        Markdown(
            content = markdown,
            modifier = modifier.padding(16.dp)
        )
    }
}

