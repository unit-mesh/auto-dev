package cc.unitmesh.devins.ui.compose.sketch

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.platform.createFileChooser
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.sourceforge.plantuml.FileFormat
import net.sourceforge.plantuml.FileFormatOption
import net.sourceforge.plantuml.SourceStringReader
import java.io.ByteArrayOutputStream


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


@Composable
fun PlantUmlRenderer(
    code: String,
    isDarkTheme: Boolean,
    modifier: Modifier,
    onRenderComplete: ((success: Boolean, message: String) -> Unit)?
) {
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var pngBytes by remember { mutableStateOf<ByteArray?>(null) }
    var svgBytes by remember { mutableStateOf<ByteArray?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showDownloadMenu by remember { mutableStateOf(false) }

    LaunchedEffect(code, isDarkTheme) {
        isLoading = true
        error = null
        try {
            withContext(Dispatchers.IO) {
                val reader = SourceStringReader(code)

                // Generate high-quality PNG with scale 4.0 for better display on 4K screens
                val pngOs = ByteArrayOutputStream()
                val pngOption = FileFormatOption(FileFormat.PNG).withScale(4.0)
                reader.generateImage(pngOs, pngOption)
                pngBytes = pngOs.toByteArray()

                // Generate SVG
                val svgOs = ByteArrayOutputStream()
                val svgOption = FileFormatOption(FileFormat.SVG)
                reader.generateImage(svgOs, svgOption)
                svgBytes = svgOs.toByteArray()
            }

            val bytes = pngBytes
            if (bytes == null || bytes.isEmpty()) {
                error = "No image generated"
                onRenderComplete?.invoke(false, "No image generated")
            } else {
                // Convert PNG bytes to ImageBitmap
                val skiaImage = org.jetbrains.skia.Image.makeFromEncoded(bytes)
                imageBitmap = skiaImage.toComposeImageBitmap()
                onRenderComplete?.invoke(true, "Success")
            }
        } catch (e: Exception) {
            error = e.message ?: "Unknown error"
            onRenderComplete?.invoke(false, error ?: "Unknown error")
        } finally {
            isLoading = false
        }
    }

    Box(modifier = modifier) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (error != null) {
            Text(text = "Error: $error", color = Color.Red, modifier = Modifier.align(Alignment.Center))
        } else {
            imageBitmap?.let { bitmap ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Image display
                    Image(
                        bitmap = bitmap,
                        contentDescription = "PlantUML Diagram",
                        contentScale = androidx.compose.ui.layout.ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Download button overlay (always visible)
                    Box(modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)) {
                        IconButton(
                            onClick = { showDownloadMenu = true },
                            modifier = Modifier
                                .size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = "Download",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showDownloadMenu,
                            onDismissRequest = { showDownloadMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Download PNG") },
                                onClick = {
                                    showDownloadMenu = false
                                    pngBytes?.let { bytes ->
                                        CoroutineScope(Dispatchers.IO).launch {
                                            saveDiagram(bytes, "png")
                                        }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Download SVG") },
                                onClick = {
                                    showDownloadMenu = false
                                    svgBytes?.let { bytes ->
                                        CoroutineScope(Dispatchers.IO).launch {
                                            saveDiagram(bytes, "svg")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private suspend fun saveDiagram(bytes: ByteArray, format: String) {
    try {
        val fileChooser = createFileChooser()
        fileChooser.saveFile(
            title = "Save PlantUML Diagram",
            defaultFileName = "plantuml-diagram.$format",
            fileExtension = format,
            data = bytes
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
