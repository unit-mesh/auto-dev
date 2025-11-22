package cc.unitmesh.devins.ui.compose.sketch

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.platform.createFileChooser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.sourceforge.plantuml.FileFormat
import net.sourceforge.plantuml.FileFormatOption
import net.sourceforge.plantuml.SourceStringReader
import org.jetbrains.skia.Image
import java.io.ByteArrayOutputStream

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
                val skiaImage = Image.makeFromEncoded(bytes)
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
            CircularProgressIndicator(modifier = Modifier.Companion.align(Alignment.Companion.Center))
        } else if (error != null) {
            Text(
                text = "Error: $error",
                color = Color.Companion.Red,
                modifier = Modifier.Companion.align(Alignment.Companion.Center)
            )
        } else {
            imageBitmap?.let { bitmap ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Image display
                    androidx.compose.foundation.Image(
                        bitmap = bitmap,
                        contentDescription = "PlantUML Diagram",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.widthIn(min = 800.dp)
                    )

                    // Download button overlay (always visible)
                    Box(modifier = Modifier.Companion.align(Alignment.Companion.BottomEnd).padding(4.dp)) {
                        IconButton(
                            onClick = { showDownloadMenu = true },
                            modifier = Modifier.Companion
                                .size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = "Download",
                                modifier = Modifier.Companion.size(18.dp)
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
