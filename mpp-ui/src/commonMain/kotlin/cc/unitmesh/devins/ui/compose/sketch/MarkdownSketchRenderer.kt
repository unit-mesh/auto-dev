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
        isDarkTheme: Boolean = false,
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
     * @param markdown Markdown 内容
     * @param isComplete 内容是否完整（流式传输时为 false）
     * @param isDarkTheme 是否使用暗色主题
     * @param modifier 修饰符
     */
    @Composable
    fun RenderMarkdown(
        markdown: String,
        isComplete: Boolean = true,
        isDarkTheme: Boolean = false,
        modifier: Modifier = Modifier
    )
}
