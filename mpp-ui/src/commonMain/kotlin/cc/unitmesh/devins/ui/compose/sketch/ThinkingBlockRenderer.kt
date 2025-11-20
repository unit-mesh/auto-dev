package cc.unitmesh.devins.ui.compose.sketch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons

/**
 * Renderer for <thinking> blocks - displays model's reasoning process
 * with special styling (gray, small text, collapsible, max 5 lines scrollable)
 * 
 * @param thinkingContent The thinking content to display
 * @param isComplete Whether the content is complete (false for streaming)
 * @param modifier Modifier for the component
 */
@Composable
fun ThinkingBlockRenderer(
    thinkingContent: String,
    isComplete: Boolean = true,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()
    var userHasScrolled by remember { mutableStateOf(false) }
    
    // Track if user manually scrolled away from bottom
    LaunchedEffect(scrollState.value, scrollState.maxValue) {
        if (scrollState.maxValue > 0) {
            val isAtBottom = scrollState.value >= scrollState.maxValue - 10 // 10dp tolerance
            if (!isAtBottom && scrollState.isScrollInProgress) {
                userHasScrolled = true
            } else if (isAtBottom) {
                userHasScrolled = false
            }
        }
    }
    
    // Auto-scroll to bottom during streaming (when not complete)
    LaunchedEffect(thinkingContent, isComplete) {
        if (!isComplete && isExpanded && thinkingContent.isNotBlank() && !userHasScrolled) {
            // Scroll to bottom when content changes during streaming
            // Only if user hasn't manually scrolled up
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isExpanded) AutoDevComposeIcons.ExpandMore else AutoDevComposeIcons.ChevronRight,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )

                Text(
                    text = "💭 Thinking process",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(4.dp))

                // Scrollable container with max 5 lines height
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 80.dp) // Approximately 5 lines
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = thinkingContent,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
