package cc.unitmesh.devins.ui.compose.sketch

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import com.mikepenz.markdown.compose.LocalMarkdownTypography
import com.mikepenz.markdown.compose.MarkdownElement
import com.mikepenz.markdown.compose.components.CurrentComponentsBridge
import com.mikepenz.markdown.compose.components.MarkdownComponents
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownCodeFence
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCode
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeBlock
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.model.DefaultMarkdownTypography
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
        isComplete: Boolean,
        isDarkTheme: Boolean,
        modifier: Modifier
    ) {
        val isDarkTheme = isSystemInDarkTheme()
        val highlightsBuilder = remember(isDarkTheme) {
            Highlights.Builder().theme(SyntaxThemes.atom(darkMode = isDarkTheme))
        }

        val typography = DefaultMarkdownTypography(
            h1 = MaterialTheme.typography.headlineMedium,
            h2 = MaterialTheme.typography.headlineSmall,
            h3 = MaterialTheme.typography.titleLarge,
            h4 = MaterialTheme.typography.titleMedium,
            h5 = MaterialTheme.typography.titleSmall,
            h6 = MaterialTheme.typography.labelLarge,
            text = MaterialTheme.typography.bodyMedium,
            code = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            quote = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
            paragraph = MaterialTheme.typography.bodyMedium,
            ordered = MaterialTheme.typography.bodyMedium,
            bullet = MaterialTheme.typography.bodyMedium,
            list = MaterialTheme.typography.bodyMedium,
            table = MaterialTheme.typography.bodyMedium,
            inlineCode = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            textLink = TextLinkStyles(
                style = SpanStyle(color = MaterialTheme.colorScheme.primary)
            )
        )

        Markdown(
            markdown,
            modifier = modifier,
            typography = typography,
            components = markdownComponents(
                table = { tableContent ->
                    val style = LocalMarkdownTypography.current.table
                    MarkdownTable(
                        content = tableContent.content,
                        node = tableContent.node,
                        style = style,
                    )
                },
                heading1 = CurrentComponentsBridge.heading3,
                heading2 = CurrentComponentsBridge.heading4,
                heading3 = CurrentComponentsBridge.heading5,
                heading4 = CurrentComponentsBridge.heading6,
                heading5 = CurrentComponentsBridge.heading6,
                codeBlock = {
                    MarkdownHighlightedCodeBlock(
                        content = it.content,
                        node = it.node,
                        highlightsBuilder = highlightsBuilder,
                        showHeader = true, // optional enable header with code language + copy button
                    )
                },
                codeFence = {
                    val style = LocalMarkdownTypography.current.code
                    MarkdownCodeFence(it.content, it.node, style) { code, language, style ->
                        val language = language?.lowercase()
                        if ((language == "plantuml" || language == "puml") && isComplete) {
                            PlantUmlRenderer(
                                code = code,
                                isDarkTheme = isDarkTheme,
                                modifier = Modifier.fillMaxSize(),
                                null
                            )
                        } else {
                            MarkdownHighlightedCode(
                                code = code,
                                language = language,
                                style = style,
                                highlightsBuilder = highlightsBuilder,
                                showHeader = true,
                            )
                        }
                    }
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


