package cc.unitmesh.devins.ui.compose.sketch

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/**
 * 渲染代码块
 */
@Composable
fun CodeBlockRenderer(
    code: String,
    language: String,
    displayName: String = language
) {
    Card(
        modifier = Modifier.Companion.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
    ) {
        Column(
            modifier =
                Modifier.Companion
                    .fillMaxWidth()
                    .padding(12.dp)
        ) {
            if (displayName.isNotEmpty() && displayName != "markdown") {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.Companion.padding(bottom = 8.dp)
                )
            }

            SelectionContainer {
                Text(
                    text = code,
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Companion.Monospace
                        ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.Companion.fillMaxWidth()
                )
            }
        }
    }
}
