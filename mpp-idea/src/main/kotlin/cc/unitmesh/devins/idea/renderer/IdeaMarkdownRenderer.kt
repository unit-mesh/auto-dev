package cc.unitmesh.devins.idea.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.idea.renderer.markdown.SimpleJewelMarkdown
import cc.unitmesh.devins.parser.CodeFence
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import com.intellij.openapi.Disposable
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text

/**
 * IntelliJ IDEA-specific Markdown renderer with Mermaid diagram support.
 * Uses Jewel components for native IntelliJ look and feel.
 * Uses multiplatform-markdown-renderer for proper markdown parsing.
 *
 * @param content The markdown content to render
 * @param isComplete Whether the content is complete (not streaming)
 * @param parentDisposable Parent disposable for JCEF resource cleanup
 * @param modifier Compose modifier
 */
@Composable
fun IdeaMarkdownRenderer(
    content: String,
    isComplete: Boolean,
    parentDisposable: Disposable,
    modifier: Modifier = Modifier
) {
    // Check if content contains mermaid code blocks
    val hasMermaid = remember(content) {
        content.contains("```mermaid") || content.contains("```mmd")
    }

    if (hasMermaid && isComplete) {
        // Use custom rendering with Mermaid support
        MermaidAwareMarkdownRenderer(
            content = content,
            parentDisposable = parentDisposable,
            modifier = modifier
        )
    } else {
        // Use simple Jewel Markdown renderer
        SimpleJewelMarkdown(
            content = content,
            modifier = modifier
        )
    }
}

/**
 * Custom markdown renderer that handles Mermaid code blocks separately.
 * Parses markdown into blocks and renders Mermaid diagrams using JCEF.
 */
@Composable
private fun MermaidAwareMarkdownRenderer(
    content: String,
    parentDisposable: Disposable,
    modifier: Modifier = Modifier
) {
    val codeFences = remember(content) { CodeFence.parseAll(content) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        codeFences.forEach { fence ->
            when (fence.languageId.lowercase()) {
                "mermaid", "mmd" -> {
                    if (fence.text.isNotBlank()) {
                        MermaidDiagramView(
                            mermaidCode = fence.text,
                            isDarkTheme = true,
                            parentDisposable = parentDisposable,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                "markdown", "md", "" -> {
                    if (fence.text.isNotBlank()) {
                        SimpleJewelMarkdown(
                            content = fence.text,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                else -> {
                    if (fence.text.isNotBlank()) {
                        CodeBlockView(
                            code = fence.text,
                            language = fence.languageId,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Code block renderer with syntax highlighting placeholder.
 */
@Composable
private fun CodeBlockView(
    code: String,
    language: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(JewelTheme.globalColors.panelBackground.copy(alpha = 0.5f))
            .padding(8.dp)
    ) {
        if (language.isNotBlank()) {
            Text(
                text = language,
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AutoDevColors.Blue.c400
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Text(
            text = code,
            style = JewelTheme.defaultTextStyle.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

