package cc.unitmesh.devins.ui.compose.agent.codereview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.compose.sketch.SketchRenderer
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors

/**
 * Represents a file link in the plan
 */
data class FileLink(
    val displayText: String,
    val filePath: String
)

/**
 * Represents a plan step with status and file links
 */
data class PlanStep(
    val text: String,
    val status: StepStatus = StepStatus.TODO,
    val fileLinks: List<FileLink> = emptyList()
)

/**
 * Step status indicator
 */
enum class StepStatus {
    TODO,       // [ ]
    COMPLETED,  // [✓]
    FAILED,     // [!]
    IN_PROGRESS // [*]
}

/**
 * Represents a modification plan item with nested steps
 */
data class PlanItem(
    val number: Int,
    val title: String,
    val priority: String,
    val steps: List<PlanStep> = emptyList()
) {
    /**
     * Get all file paths from this plan item
     */
    fun getAllFilePaths(): List<String> {
        return steps.flatMap { it.fileLinks.map { link -> link.filePath } }.distinct()
    }
}


/**
 * Displays AI-generated modification plan with modern Plan-like UI
 * 
 * @param planOutput The plan markdown output
 * @param isActive Whether the plan is being generated
 * @param selectedItems Set of selected plan item numbers (for multi-select)
 * @param onItemSelectionChanged Callback when selection changes
 * @param onFileLinkClick Callback when a file link is clicked
 * @param modifier Modifier for the component
 */
@Composable
fun ModificationPlanSection(
    planOutput: String,
    isActive: Boolean,
    selectedItems: Set<Int> = emptySet(),
    onItemSelectionChanged: (Set<Int>) -> Unit = {},
    onFileLinkClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    val planItems = remember(planOutput) {
        if (!isActive && planOutput.isNotBlank()) {
            PlanParser.parse(planOutput)
        } else {
            emptyList()
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                AutoDevColors.Indigo.c600.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) AutoDevComposeIcons.ExpandMore else AutoDevComposeIcons.ChevronRight,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )

                    Icon(
                        imageVector = AutoDevComposeIcons.Article,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )

                    Text(
                        text = "修改计划",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (planItems.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${planItems.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (selectedItems.isNotEmpty()) {
                        Surface(
                            color = AutoDevColors.Blue.c600,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${selectedItems.size} 已选",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (isActive) {
                        Surface(
                            color = AutoDevColors.Indigo.c600,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "生成中",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isActive || planItems.isEmpty()) {
                        // Show raw markdown during generation or if parsing failed
                        if (planOutput.isNotBlank()) {
                            SketchRenderer.RenderResponse(
                                content = planOutput,
                                isComplete = !isActive,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        // Show structured plan items
                        planItems.forEach { item ->
                            PlanItemCard(
                                item = item,
                                isSelected = selectedItems.contains(item.number),
                                onSelectionChanged = { selected ->
                                    val newSelection = if (selected) {
                                        selectedItems + item.number
                                    } else {
                                        selectedItems - item.number
                                    }
                                    onItemSelectionChanged(newSelection)
                                },
                                onFileLinkClick = onFileLinkClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanItemCard(
    item: PlanItem,
    isSelected: Boolean,
    onSelectionChanged: (Boolean) -> Unit,
    onFileLinkClick: ((String) -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(true) }
    
    // Get adjusted priority using utility function
    val adjustedPriority = remember(item) {
        PlanPriority.getAdjustedPriority(item)
    }
    
    // Get priority color based on adjusted priority
    val priorityColor = when {
        adjustedPriority.contains("关键") || adjustedPriority.contains("CRITICAL") -> AutoDevColors.Red.c600
        adjustedPriority.contains("高") || adjustedPriority.contains("HIGH") -> AutoDevColors.Amber.c600
        adjustedPriority.contains("中等") || adjustedPriority.contains("MEDIUM") -> AutoDevColors.Blue.c600
        adjustedPriority.contains("低") || adjustedPriority.contains("LOW") -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = AutoDevColors.Blue.c600,
                        shape = RoundedCornerShape(6.dp)
                    )
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> AutoDevColors.Blue.c600.copy(alpha = 0.05f)
                else -> priorityColor.copy(alpha = 0.03f) // Subtle background tint based on priority
            }
        ),
        shape = RoundedCornerShape(6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) {
                AutoDevColors.Blue.c600.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Item header with checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        onSelectionChanged(!isSelected)
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Checkbox for selection
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { 
                            onSelectionChanged(it)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    
                    // Expand/collapse icon
                    Icon(
                        imageVector = if (isExpanded) AutoDevComposeIcons.ExpandMore else AutoDevComposeIcons.ChevronRight,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable(
                                onClick = { isExpanded = !isExpanded },
                                indication = null,
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            )
                    )
                    
                    Text(
                        text = "${item.number}. ${item.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Surface(
                    color = priorityColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = adjustedPriority,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = priorityColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Item steps
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item.steps.forEach { step ->
                        PlanStepItem(
                            step = step,
                            onFileLinkClick = onFileLinkClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanStepItem(
    step: PlanStep,
    onFileLinkClick: ((String) -> Unit)? = null
) {
    val statusIcon = when (step.status) {
        StepStatus.COMPLETED -> AutoDevComposeIcons.CheckCircle
        StepStatus.FAILED -> AutoDevComposeIcons.Error
        StepStatus.IN_PROGRESS -> AutoDevComposeIcons.Refresh
        StepStatus.TODO -> AutoDevComposeIcons.CheckBoxOutlineBlank
    }
    
    val statusColor = when (step.status) {
        StepStatus.COMPLETED -> AutoDevColors.Green.c600
        StepStatus.FAILED -> AutoDevColors.Red.c600
        StepStatus.IN_PROGRESS -> AutoDevColors.Blue.c600
        StepStatus.TODO -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = statusIcon,
            contentDescription = step.status.name,
            tint = statusColor,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Step text with clickable file links
            val annotatedText = buildAnnotatedString {
                if (step.fileLinks.isEmpty()) {
                    // No file links, just append the text
                    append(step.text)
                } else {
                    var remainingText = step.text
                    var lastIndex = 0
                    
                    // Replace file links with annotated spans
                    step.fileLinks.forEach { link ->
                        val linkPattern = "[${link.displayText}](${link.filePath})"
                        val linkIndex = remainingText.indexOf(linkPattern, lastIndex)
                        
                        if (linkIndex >= 0) {
                            // Add text before link
                            if (linkIndex > lastIndex) {
                                append(remainingText.substring(lastIndex, linkIndex))
                            }
                            
                            // Add link with style and clickable annotation
                            val start = length
                            append(link.displayText)
                            val end = length
                            
                            // Add string annotation for file link
                            addStringAnnotation(
                                tag = "FILE_LINK",
                                annotation = link.filePath,
                                start = start,
                                end = end
                            )
                            
                            addStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline,
                                    fontWeight = FontWeight.Medium
                                ),
                                start = start,
                                end = end
                            )
                            
                            lastIndex = linkIndex + linkPattern.length
                        }
                    }
                    
                    // Add remaining text
                    if (lastIndex < remainingText.length) {
                        append(remainingText.substring(lastIndex))
                    }
                }
            }
            
            // Use ClickableText for file link support (deprecated but still works)
            androidx.compose.foundation.text.ClickableText(
                text = annotatedText,
                style = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.2
                ),
                onClick = { offset ->
                    annotatedText.getStringAnnotations(
                        tag = "FILE_LINK",
                        start = offset,
                        end = offset
                    ).firstOrNull()?.let { annotation ->
                        onFileLinkClick?.invoke(annotation.item)
                    }
                }
            )
        }
    }
}
