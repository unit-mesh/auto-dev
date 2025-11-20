package cc.unitmesh.devins.ui.compose.sketch

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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Render the walkthrough content as markdown
            // Since the content is structured markdown, we use TextBlockRenderer
            // which will handle the markdown rendering including tables and diagrams
            TextBlockRenderer(walkthroughContent)
        }
    }
}

