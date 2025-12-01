package cc.unitmesh.devins.ui.compose.sketch

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font

/**
 * JVM implementation of FiraCode font loading
 * Loads fonts from commonMain/resources/fonts/
 */
actual fun getFiraCodeFontFamily(): FontFamily {
    return try {
        FontFamily(
            Font(
                resource = "fonts/FiraCode-Regular.ttf",
                weight = FontWeight.Normal
            ),
            Font(
                resource = "fonts/FiraCode-Medium.ttf",
                weight = FontWeight.Medium
            ),
            Font(
                resource = "fonts/FiraCode-Bold.ttf",
                weight = FontWeight.Bold
            )
        )
    } catch (e: Exception) {
        println("Failed to load FiraCode font: ${e.message}, falling back to default monospace")
        FontFamily.Monospace
    }
}

/**
 * JVM implementation - use default font family
 * JVM has good system font support for UTF-8
 */
@Composable
actual fun getUtf8FontFamily(): FontFamily = FontFamily.Default
