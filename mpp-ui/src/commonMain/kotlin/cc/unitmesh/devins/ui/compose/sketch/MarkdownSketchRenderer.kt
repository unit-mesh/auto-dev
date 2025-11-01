package cc.unitmesh.devins.ui.compose.sketch

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Markdown Sketch 渲染器接口
 * 
 * 使用 expect/actual 机制为不同平台提供不同的实现：
 * - JVM/Android: 使用 Compose + multiplatform-markdown-renderer
 * - JS: 使用纯文本渲染（不依赖 Compose UI，因为在 CLI 中不需要）
 */
expect object MarkdownSketchRenderer {
    /**
     * 渲染 LLM 响应内容
     * 使用 CodeFence.parseAll() 解析内容，然后根据类型渲染
     */
    @Composable
    fun RenderResponse(
        content: String,
        isComplete: Boolean = false,
        modifier: Modifier = Modifier
    )
    
    /**
     * 纯文本渲染（不解析 Markdown）
     */
    @Composable
    fun RenderPlainText(
        text: String,
        modifier: Modifier = Modifier
    )
    
    /**
     * 渲染单个 Markdown 内容块（不使用 CodeFence 解析）
     */
    @Composable
    fun RenderMarkdown(
        markdown: String,
        modifier: Modifier = Modifier
    )
}


