package cc.unitmesh.devins.ui.compose.sketch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.delay
import org.intellij.markdown.ast.ASTNode


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
    var showFileViewerDialog by remember { mutableStateOf(false) }
    var selectedFilePath by remember { mutableStateOf<String?>(null) }

    CompositionLocalProvider(
        LocalOnOpenFile provides { filePath ->
            selectedFilePath = filePath
            showFileViewerDialog = true
        }
    ) {
        Column(modifier) {
            state.node.children.forEach { node ->
                MarkdownElementWithFileLinks(
                    node = node,
                    components = components,
                    content = state.content
                )
            }
        }
    }

    // Show file viewer dialog
    if (showFileViewerDialog && selectedFilePath != null) {
        cc.unitmesh.devins.ui.compose.agent.codereview.FileViewerDialog(
            filePath = selectedFilePath!!,
            onClose = { showFileViewerDialog = false }
        )
    }
}

fun safeSubstring(content: String, start: Int, end: Int): String {
    val safeStart = start.coerceIn(0, content.length)
    val safeEnd = end.coerceIn(safeStart, content.length)
    return content.substring(safeStart, safeEnd)
}

/**
 * Custom MarkdownElement that handles file:// links with icons
 */
@Composable
fun MarkdownElementWithFileLinks(
    node: ASTNode,
    components: MarkdownComponents,
    content: String
) {
    val onOpenFile = LocalOnOpenFile.current

    if (node.type == org.intellij.markdown.MarkdownElementTypes.INLINE_LINK) {
        val linkDestination = node.children.firstOrNull {
            it.type == org.intellij.markdown.MarkdownElementTypes.LINK_DESTINATION
        }
        val linkText = node.children.firstOrNull {
            it.type == org.intellij.markdown.MarkdownElementTypes.LINK_TEXT
        }

        val url = linkDestination?.let { safeSubstring(content, it.startOffset, it.endOffset) } ?: ""
        val text = linkText?.let {
            safeSubstring(content, it.startOffset, it.endOffset).removeSurrounding("[", "]")
        } ?: url

        if (url.startsWith("file://")) {
            val filePath = url.removePrefix("file://")

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable {
                        val root = cc.unitmesh.devins.workspace.WorkspaceManager.getCurrentOrEmpty().rootPath
                        val absolutePath = if (filePath.startsWith("/")) {
                            filePath
                        } else if (root != null) {
                            "$root/$filePath"
                        } else {
                            filePath
                        }
                        onOpenFile(absolutePath)
                    }
                    .padding(horizontal = 2.dp, vertical = 1.dp)
            ) {
                Icon(
                    imageVector = cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons.Visibility,
                    contentDescription = "View File",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                androidx.compose.material3.Text(
                    text = text,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                    )
                )
            }
            return
        }
    }

    MarkdownElement(node, components, content)
}

