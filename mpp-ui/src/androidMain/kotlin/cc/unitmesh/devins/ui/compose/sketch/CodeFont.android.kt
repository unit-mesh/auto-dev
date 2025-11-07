package cc.unitmesh.devins.ui.compose.sketch

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import cc.unitmesh.devins.ui.R

/**
 * Android implementation of FiraCode font loading
 * Loads fonts from androidMain/res/font/
 */
actual fun getFiraCodeFontFamily(): FontFamily {
    return try {
        FontFamily(
            Font(
                resId = R.font.firacode_regular,
                weight = FontWeight.Normal
            ),
            Font(
                resId = R.font.firacode_medium,
                weight = FontWeight.Medium
            ),
            Font(
                resId = R.font.firacode_bold,
                weight = FontWeight.Bold
            )
        )
    } catch (e: Exception) {
        println("Failed to load FiraCode font: ${e.message}, falling back to default monospace")
        FontFamily.Monospace
    }
}

