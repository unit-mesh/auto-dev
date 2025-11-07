package cc.unitmesh.devins.ui.compose.sketch

import androidx.compose.ui.text.font.FontFamily

/**
 * JS implementation of FiraCode font loading
 * Falls back to default monospace font (browser will use system monospace)
 * 
 * Note: For web platforms, FiraCode can be loaded via CSS @font-face
 * or served from a CDN like Google Fonts or jsDelivr
 */
actual fun getFiraCodeFontFamily(): FontFamily {
    // For JS/Web, we rely on CSS font definitions
    // The browser will use the system's monospace font
    return FontFamily.Monospace
}

