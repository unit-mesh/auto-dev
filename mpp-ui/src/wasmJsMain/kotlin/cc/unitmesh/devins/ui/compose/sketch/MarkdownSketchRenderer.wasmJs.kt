package cc.unitmesh.devins.ui.compose.sketch

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.parser.CodeFence
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
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * WASM 平台的 Markdown Sketch 渲染器实现
 * 使用 Compose + multiplatform-markdown-renderer
 * Mermaid 图表使用 Kroki 远程渲染
 */
actual object MarkdownSketchRenderer {
    @Composable
    actual fun RenderMarkdown(
        markdown: String,
        isComplete: Boolean,
        isDarkTheme: Boolean,
        modifier: Modifier
    ) {
        val isSystemDark = isSystemInDarkTheme()
        val highlightsBuilder = remember(isSystemDark) {
            Highlights.Builder().theme(SyntaxThemes.atom(darkMode = isSystemDark))
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

        Column(modifier = modifier) {
            Markdown(
                markdown,
                modifier = Modifier.fillMaxWidth(),
                typography = typography,
                components = markdownComponents(
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
                            showHeader = true,
                        )
                    },
                    codeFence = {
                        val style = LocalMarkdownTypography.current.code
                        MarkdownCodeFence(it.content, it.node, style) { code, language, style ->
                            // Render Mermaid diagrams using remote Kroki service
                            if (language?.lowercase() == "mermaid" && isComplete) {
                                RemoteMermaidRenderer(
                                    mermaidCode = code,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp)
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

            if (!isComplete) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }

    @Composable
    actual fun RenderResponse(content: String, isComplete: Boolean, isDarkTheme: Boolean, modifier: Modifier) {
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
            RenderMarkdown(content, isComplete, isDarkTheme, Modifier.fillMaxWidth())
        }
    }

    @Composable
    actual fun RenderPlainText(text: String, modifier: Modifier) {
        Text(
            text = text,
            modifier = modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
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

@OptIn(ExperimentalEncodingApi::class, ExperimentalResourceApi::class)
@Composable
fun RemoteMermaidRenderer(
    mermaidCode: String,
    modifier: Modifier = Modifier
) {
    var imageBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(mermaidCode) {
        isLoading = true
        error = null
        try {
            val base64Code = Base64.UrlSafe.encode(mermaidCode.encodeToByteArray())
            val url = "https://kroki.io/mermaid/png/$base64Code"

            val client = HttpClient()
            val response: HttpResponse = client.get(url)
            val bytes = response.readBytes()
            
            // Decode PNG bytes to ImageBitmap
            // Note: decodeToImageBitmap is from compose-resources
            imageBitmap = bytes.decodeToImageBitmap()
        } catch (e: Exception) {
            error = "Failed to render diagram: ${e.message}"
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (error != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Error rendering diagram", color = MaterialTheme.colorScheme.error)
                Text(error ?: "", style = MaterialTheme.typography.bodySmall)
                // Fallback to showing code
                Text(mermaidCode, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(8.dp))
            }
        } else {
            imageBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap,
                    contentDescription = "Mermaid Diagram",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
