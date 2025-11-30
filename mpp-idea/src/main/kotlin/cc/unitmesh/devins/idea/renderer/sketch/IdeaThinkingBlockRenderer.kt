package cc.unitmesh.devins.idea.renderer.sketch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys

/**
 * Thinking block renderer for IntelliJ IDEA with Jewel styling.
 * Displays model's reasoning process in a collapsible, scrollable container.
 */
@Composable
fun IdeaThinkingBlockRenderer(
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
            val isAtBottom = scrollState.value >= scrollState.maxValue - 10
            if (!isAtBottom && scrollState.isScrollInProgress) {
                userHasScrolled = true
            } else if (isAtBottom) {
                userHasScrolled = false
            }
        }
    }

    // Auto-scroll to bottom during streaming
    LaunchedEffect(thinkingContent) {
        if (!isComplete && isExpanded && !userHasScrolled && thinkingContent.isNotBlank()) {
            kotlinx.coroutines.delay(16)
            val targetScroll = scrollState.maxValue
            if (targetScroll > scrollState.value) {
                scrollState.scrollTo(targetScroll)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(JewelTheme.globalColors.panelBackground.copy(alpha = 0.3f))
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header with expand/collapse toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    key = if (isExpanded) AllIconsKeys.General.ArrowDown else AllIconsKeys.General.ArrowRight,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.size(12.dp),
                    tint = JewelTheme.globalColors.text.info.copy(alpha = 0.6f)
                )

                Text(
                    text = "Thinking process",
                    style = JewelTheme.defaultTextStyle.copy(
                        fontSize = 11.sp,
                        color = JewelTheme.globalColors.text.info.copy(alpha = 0.7f)
                    )
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(4.dp))

                // Scrollable content (max ~5 lines)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 80.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = thinkingContent,
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 11.sp,
                            color = JewelTheme.globalColors.text.info.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        }
    }
}

