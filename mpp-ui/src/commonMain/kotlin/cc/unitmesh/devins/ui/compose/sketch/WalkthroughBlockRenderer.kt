package cc.unitmesh.devins.ui.compose.sketch

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Renderer for <!-- walkthrough_start --> ... <!-- walkthrough_end --> blocks
 *
 * These blocks contain structured code review summaries with:
 * - Walkthrough section (2-3 paragraphs)
 * - Changes table (markdown table)
 * - Optional sequence diagrams
 *
 * The content is rendered as markdown with special styling to highlight
 * the structured nature of the walkthrough.
 */
@Composable
fun WalkthroughBlockRenderer(
    walkthroughContent: String,
    modifier: Modifier = Modifier,
    isComplete: Boolean = true
) {
    val isDarkTheme = isSystemInDarkTheme()

    Box(modifier = modifier.fillMaxWidth()) {
        MarkdownSketchRenderer.RenderMarkdown(
            markdown = walkthroughContent,
            isComplete = isComplete,
            isDarkTheme = isDarkTheme,
            modifier = modifier
        )
    }
}

