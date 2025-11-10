package cc.unitmesh.devins.ui.compose.sketch

import androidx.compose.ui.text.font.FontFamily

/**
 * iOS implementation of FiraCode font loading
 * Falls back to default monospace font
 */
actual fun getFiraCodeFontFamily(): FontFamily {
    // iOS uses system monospace font by default
    // Custom font loading would require platform-specific setup
    return FontFamily.Monospace
}

