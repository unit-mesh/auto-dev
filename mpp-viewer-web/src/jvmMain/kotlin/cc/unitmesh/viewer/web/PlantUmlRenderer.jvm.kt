package cc.unitmesh.viewer.web

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadSvgPainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.sourceforge.plantuml.FileFormat
import net.sourceforge.plantuml.FileFormatOption
import net.sourceforge.plantuml.SourceStringReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@Composable
actual fun PlantUmlRenderer(
    code: String,
    isDarkTheme: Boolean,
    modifier: Modifier,
    onRenderComplete: ((success: Boolean, message: String) -> Unit)?
) {
    var painter by remember { mutableStateOf<Painter?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val density = LocalDensity.current

    LaunchedEffect(code, isDarkTheme) {
        isLoading = true
        error = null
        try {
            val svgBytes = withContext(Dispatchers.IO) {
                val reader = SourceStringReader(code)
                val os = ByteArrayOutputStream()
                // 0 is the index of the image (usually 0)
                reader.generateImage(os, FileFormatOption(FileFormat.SVG))
                os.toByteArray()
            }

            if (svgBytes.isEmpty()) {
                error = "No image generated"
                onRenderComplete?.invoke(false, "No image generated")
            } else {
                val inputStream = ByteArrayInputStream(svgBytes)
                // loadSvgPainter requires an InputStream
                val p = loadSvgPainter(inputStream, density)
                painter = p
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
            painter?.let {
                Image(
                    painter = it,
                    contentDescription = "PlantUML Diagram",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
