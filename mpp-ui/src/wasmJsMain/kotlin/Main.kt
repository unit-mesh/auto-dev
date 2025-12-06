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
import androidx.compose.ui.window.ComposeViewport
import cc.unitmesh.mpp_ui.generated.resources.NotoSansSC_Regular
import cc.unitmesh.mpp_ui.generated.resources.Res
import cc.unitmesh.devins.ui.compose.AutoDevApp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.configureWebResources
import org.jetbrains.compose.resources.preloadFont

/**
 * WASM JS entry point with full UTF-8 font support
 *
 * Reference: https://github.com/JetBrains/compose-multiplatform/blob/master/components/resources/demo/shared/src/webMain/kotlin/main.wasm.kt
 *
 * This implementation preloads Noto Sans CJK SC to support comprehensive UTF-8 characters:
 * - Chinese (Simplified & Traditional)
 * - Japanese (Hiragana, Katakana, Kanji)
 * - Korean (Hangul)
 * - Latin, Cyrillic, Greek
 * - Emoji and symbols
 *
 * The font (~15MB) is auto-downloaded by Gradle and not committed to Git.
 * It's loaded asynchronously and the app shows a loading indicator until fonts are ready.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalResourceApi::class, InternalComposeUiApi::class)
@Suppress("DEPRECATION")
fun main() {
    // Configure web resources path mapping (required for WASM)
    configureWebResources {
        resourcePathMapping { path -> "./$path" }
    }

    ComposeViewport {
        val utf8Font = preloadFont(Res.font.NotoSansSC_Regular).value
        var fontsFallbackInitialized by remember { mutableStateOf(false) }

        AutoDevApp()

        // Show app content when fonts are loaded, otherwise show loading indicator
        if (utf8Font == null || fontsFallbackInitialized) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.8f))
                    .clickable { /* Prevent interaction while loading */ }
            ) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }

        val fontFamilyResolver = LocalFontFamilyResolver.current
        LaunchedEffect(fontFamilyResolver, utf8Font) {
            if (utf8Font != null) {
                // Preload font family to support all UTF-8 characters globally
                fontFamilyResolver.preload(FontFamily(listOf(utf8Font)))
            }
        }
    }
}

