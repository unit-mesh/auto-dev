package cc.unitmesh.devins.idea.renderer.markdown

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.mikepenz.markdown.model.DefaultMarkdownColors
import com.mikepenz.markdown.model.MarkdownColors
import org.jetbrains.jewel.foundation.theme.JewelTheme

/**
 * Creates a [MarkdownColors] instance using Jewel theme colors.
 * Provides IntelliJ-native color scheme for markdown rendering.
 */
@Composable
fun jewelMarkdownColor(
    text: Color = JewelTheme.globalColors.text.normal,
    codeBackground: Color = JewelTheme.globalColors.panelBackground.copy(alpha = 0.5f),
    inlineCodeBackground: Color = codeBackground,
    dividerColor: Color = JewelTheme.globalColors.borders.normal,
    tableBackground: Color = JewelTheme.globalColors.panelBackground.copy(alpha = 0.3f),
): MarkdownColors = DefaultMarkdownColors(
    text = text,
    codeBackground = codeBackground,
    inlineCodeBackground = inlineCodeBackground,
    dividerColor = dividerColor,
    tableBackground = tableBackground,
)

