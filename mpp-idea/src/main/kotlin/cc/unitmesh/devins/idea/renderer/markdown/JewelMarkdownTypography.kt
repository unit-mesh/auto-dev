package cc.unitmesh.devins.idea.renderer.markdown

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import com.mikepenz.markdown.model.DefaultMarkdownTypography
import com.mikepenz.markdown.model.MarkdownTypography
import org.jetbrains.jewel.foundation.theme.JewelTheme

/**
 * Creates a [MarkdownTypography] instance using Jewel theme typography.
 * Provides IntelliJ-native typography styles for markdown rendering.
 */
@Composable
fun jewelMarkdownTypography(
    baseStyle: TextStyle = JewelTheme.defaultTextStyle,
    h1: TextStyle = baseStyle.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    h2: TextStyle = baseStyle.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    h3: TextStyle = baseStyle.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
    h4: TextStyle = baseStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    h5: TextStyle = baseStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    h6: TextStyle = baseStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    text: TextStyle = baseStyle.copy(fontSize = 14.sp),
    code: TextStyle = baseStyle.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
    inlineCode: TextStyle = text.copy(fontFamily = FontFamily.Monospace),
    quote: TextStyle = baseStyle.copy(
        fontStyle = FontStyle.Italic,
        fontSize = 14.sp,
        color = JewelTheme.globalColors.text.info
    ),
    paragraph: TextStyle = text,
    ordered: TextStyle = text,
    bullet: TextStyle = text,
    list: TextStyle = text,
    textLink: TextLinkStyles = TextLinkStyles(
        style = SpanStyle(
            color = AutoDevColors.Blue.c400,
            fontWeight = FontWeight.Medium,
            textDecoration = TextDecoration.Underline
        )
    ),
    table: TextStyle = text,
): MarkdownTypography = DefaultMarkdownTypography(
    h1 = h1,
    h2 = h2,
    h3 = h3,
    h4 = h4,
    h5 = h5,
    h6 = h6,
    text = text,
    quote = quote,
    code = code,
    inlineCode = inlineCode,
    paragraph = paragraph,
    ordered = ordered,
    bullet = bullet,
    list = list,
    textLink = textLink,
    table = table,
)

