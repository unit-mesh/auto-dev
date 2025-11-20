package cc.unitmesh.devins.ui.compose.sketch

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mikepenz.markdown.compose.MarkdownElement
import com.mikepenz.markdown.compose.components.MarkdownComponents
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.compose.elements.MarkdownTable
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.model.State
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes

/**
 * JVM 平台的 Markdown Sketch 渲染器实现
 * 使用 Compose + multiplatform-markdown-renderer
 */
actual object MarkdownSketchRenderer {
    @Composable
    actual fun RenderMarkdown(
        markdown: String,
        isDarkTheme: Boolean,
        modifier: Modifier
    ) {
        val highlightsBuilder = Highlights.Builder().theme(SyntaxThemes.atom(darkMode = isDarkTheme))
        Markdown(
            markdown,
            modifier = modifier,
            components = markdownComponents(
                table = {
                    println(it.content)
                    MarkdownTable(it.content, it.node, style = it.typography.table)
                },
                codeBlock = {
                    MarkdownHighlightedCodeBlock(
                        content = it.content,
                        node = it.node,
                        highlightsBuilder = highlightsBuilder,
                        showHeader = true,
                    )
                },
                codeFence = {
                    println(it.content)
                    MarkdownHighlightedCodeFence(
                        content = it.content,
                        node = it.node,
                        highlightsBuilder = highlightsBuilder,
                        showHeader = true,
                    )
                },
            ),
            success = { state, components, modifier ->
                MarkdownSuccess(state, components, modifier)
            },
        )
    }

    @Composable
    actual fun RenderResponse(content: String, isComplete: Boolean, isDarkTheme: Boolean, modifier: Modifier) {
    }

    @Composable
    actual fun RenderPlainText(text: String, modifier: Modifier) {
    }
}

@Composable
fun MarkdownSuccess(
    state: State.Success,
    components: MarkdownComponents,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        state.node.children.forEach { node ->
            MarkdownElement(node, components, state.content)
        }
    }
}
