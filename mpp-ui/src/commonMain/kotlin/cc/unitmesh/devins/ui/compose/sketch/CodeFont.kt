package cc.unitmesh.devins.ui.compose.sketch

import androidx.compose.ui.text.font.FontFamily

/**
 * Get FiraCode font family for code blocks
 * Platform-specific implementation via expect/actual
 */
expect fun getFiraCodeFontFamily(): FontFamily

/**
 * Get default monospace font family as fallback
 */
fun getDefaultMonospaceFontFamily(): FontFamily = FontFamily.Monospace

