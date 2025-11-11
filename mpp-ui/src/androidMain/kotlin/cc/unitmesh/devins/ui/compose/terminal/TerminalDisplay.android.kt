package cc.unitmesh.devins.ui.compose.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/**
 * Android implementation of platform-specific terminal display
 * Uses ANSI terminal renderer for proper color and formatting support
 *
 * Note: This component is designed to be used inside LazyColumn,
 * so it does NOT include scroll modifiers to avoid nested scrolling conflicts.
 */
@Composable
actual fun PlatformTerminalDisplay(
    output: String,
    modifier: Modifier
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(8.dp)
    ) {
        AnsiTerminalRenderer(
            ansiText = output
        )
    }
}
