package cc.unitmesh.devins.ui.compose.sketch

import androidx.compose.runtime.Composable
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

/**
 * Get UTF-8 font family for text display
 * On WASM, returns Noto Sans SC for CJK support
 * On other platforms, returns default font family
 */
@Composable
expect fun getUtf8FontFamily(): FontFamily
