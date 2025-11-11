package cc.unitmesh.devins.ui.compose.sketch

import androidx.compose.ui.text.font.FontFamily

/**
 * WASM implementation of FiraCode font loading
 * Falls back to default monospace font
 */
actual fun getFiraCodeFontFamily(): FontFamily {
    return FontFamily.Monospace
}

