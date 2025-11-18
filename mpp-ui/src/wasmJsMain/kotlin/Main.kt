import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.window.CanvasBasedWindow
import autodev_intellij.mpp_ui.generated.resources.NotoColorEmoji
import autodev_intellij.mpp_ui.generated.resources.Res
import cc.unitmesh.devins.ui.compose.AutoDevApp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.configureWebResources
import org.jetbrains.compose.resources.preloadFont

/**
 * WASM JS entry point with Emoji/UTF-8 font support
 *
 * Reference: https://github.com/JetBrains/compose-multiplatform/blob/master/components/resources/demo/shared/src/webMain/kotlin/main.wasm.kt
 *
 * This implementation preloads NotoColorEmoji.ttf to support emoji and UTF-8 characters in WASM.
 * The font is loaded asynchronously and the app shows a loading indicator until fonts are ready.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalResourceApi::class, InternalComposeUiApi::class)
fun main() {
    // Configure web resources path mapping (required for WASM)
    configureWebResources {
        resourcePathMapping { path -> "./$path" }
    }

    CanvasBasedWindow(canvasElementId = "ComposeTarget") {
        // Preload emoji font for UTF-8 support (emoji, Chinese, Japanese, etc.)
        val emojiFont = preloadFont(Res.font.NotoColorEmoji).value
        println("Emoji font loaded: $emojiFont")
        var fontsFallbackInitialized by remember { mutableStateOf(false) }

        AutoDevApp()

        if (emojiFont != null && fontsFallbackInitialized) {
            println("Fonts are ready")
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.8f))
            ) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            println("Fonts are not ready yet")
        }

        val fontFamilyResolver = LocalFontFamilyResolver.current
        LaunchedEffect(fontFamilyResolver, emojiFont) {
            if (emojiFont != null) {
                fontFamilyResolver.preload(FontFamily(listOf(emojiFont)))
                fontsFallbackInitialized = true
            }
        }
    }
}

