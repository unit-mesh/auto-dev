package cc.unitmesh.devins.ui.compose.terminal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-agnostic terminal output display.
 * On JVM: Uses JediTerm for rich terminal rendering
 * On other platforms: Falls back to simple text display
 */
@Composable
expect fun PlatformTerminalDisplay(
    output: String,
    modifier: Modifier = Modifier
)
