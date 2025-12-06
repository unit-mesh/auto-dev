package cc.unitmesh.devins.ui.compose.sketch

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import cc.unitmesh.mpp_ui.generated.resources.NotoSansSC_Regular
import cc.unitmesh.mpp_ui.generated.resources.Res
import org.jetbrains.compose.resources.Font

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

/**
 * WASM implementation - use Noto Sans SC for CJK support
 * This font is only bundled in WASM builds (~18MB)
 */
@Composable
actual fun getUtf8FontFamily(): FontFamily {
    return FontFamily(Font(Res.font.NotoSansSC_Regular))
}
