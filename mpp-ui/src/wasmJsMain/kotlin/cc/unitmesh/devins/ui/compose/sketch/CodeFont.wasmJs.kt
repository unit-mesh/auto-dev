package cc.unitmesh.devins.ui.compose.sketch

import androidx.compose.ui.text.font.FontFamily

/**
 * WASM JS implementation of FiraCode font loading
 * Falls back to default monospace
 * 
 * Note: This is non-composable to match the expect declaration in commonMain
 */
actual fun getFiraCodeFontFamily(): FontFamily {
    // WASM uses default monospace for code
    return FontFamily.Monospace
}
