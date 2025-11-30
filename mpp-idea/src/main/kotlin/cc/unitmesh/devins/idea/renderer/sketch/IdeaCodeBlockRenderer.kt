package cc.unitmesh.devins.idea.renderer.sketch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text

/**
 * Code block renderer for IntelliJ IDEA with Jewel styling.
 */
@Composable
fun IdeaCodeBlockRenderer(
    code: String,
    language: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(JewelTheme.globalColors.panelBackground.copy(alpha = 0.5f))
            .padding(8.dp)
    ) {
        // Language header
        if (language.isNotBlank()) {
            Text(
                text = language,
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AutoDevColors.Blue.c400
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Code content
        Text(
            text = code,
            style = JewelTheme.defaultTextStyle.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

