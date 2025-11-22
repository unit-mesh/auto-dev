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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.sourceforge.plantuml.FileFormat
import net.sourceforge.plantuml.FileFormatOption
import net.sourceforge.plantuml.SourceStringReader
import org.jetbrains.skia.Image as SkiaImage
import java.io.ByteArrayOutputStream

@Composable
actual fun PlantUmlRenderer(
    code: String,
    isDarkTheme: Boolean,
    modifier: Modifier,
    onRenderComplete: ((success: Boolean, message: String) -> Unit)?
) {
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(code, isDarkTheme) {
        isLoading = true
        error = null
        try {
            val pngBytes = withContext(Dispatchers.IO) {
                val reader = SourceStringReader(code)
                val os = ByteArrayOutputStream()
                // Use PNG format for better font rendering
                reader.generateImage(os, FileFormatOption(FileFormat.PNG))
                os.toByteArray()
            }

            if (pngBytes.isEmpty()) {
                error = "No image generated"
                onRenderComplete?.invoke(false, "No image generated")
            } else {
                // Convert PNG bytes to ImageBitmap
                val skiaImage = SkiaImage.makeFromEncoded(pngBytes)
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
            imageBitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = "PlantUML Diagram",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
