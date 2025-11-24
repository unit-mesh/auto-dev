package cc.unitmesh.devins.ui.compose.agent.codereview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
)

/**
 * Parses Plan format markdown into structured plan items
 * Supports nested structure: ordered list for items, unordered list for steps
 */
object PlanParser {
    private val CODE_BLOCK_PATTERN = Regex("```plan\\s*\\n([\\s\\S]*?)\\n```", RegexOption.IGNORE_CASE)
    private val ORDERED_ITEM_PATTERN = Regex("^(\\d+)\\.\\s*(.+?)(?:\\s*-\\s*(.+))?$")
    private val UNORDERED_ITEM_PATTERN = Regex("^\\s*-\\s*\\[([\\s✓!*]?)\\]\\s*(.+)$")
    private val FILE_LINK_PATTERN = Regex("\\[([^\\]]+)\\]\\(([^)]+)\\)")

    fun parse(planOutput: String): List<PlanItem> {
        val items = mutableListOf<PlanItem>()
        
        // Extract plan code block if present
        val planContent = CODE_BLOCK_PATTERN.find(planOutput)?.groupValues?.get(1) 
            ?: planOutput
        
        val lines = planContent.lines()
        var currentItem: PlanItem? = null
        var currentSteps = mutableListOf<PlanStep>()
        var currentNumber = 0
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            
            // Check for ordered list item (main plan item)
            val orderedMatch = ORDERED_ITEM_PATTERN.find(trimmed)
            if (orderedMatch != null) {
                // Save previous item
                currentItem?.let {
                    items.add(it.copy(steps = currentSteps.toList()))
                }
                
                // Start new item
                currentNumber = orderedMatch.groupValues[1].toIntOrNull() ?: 0
                val titleWithPriority = orderedMatch.groupValues[2].trim()
                
                // Extract priority from title (format: "Title - PRIORITY")
                val titleParts = titleWithPriority.split(" - ", limit = 2)
                val title = titleParts[0].trim()
                val priority = titleParts.getOrNull(1)?.trim() ?: "MEDIUM"
                
                currentItem = PlanItem(
                    number = currentNumber,
                    title = title,
                    priority = priority
                )
                currentSteps = mutableListOf()
                continue
            }
            
            // Check for unordered list item (plan step)
            val unorderedMatch = UNORDERED_ITEM_PATTERN.find(trimmed)
            if (unorderedMatch != null) {
                val statusMarker = unorderedMatch.groupValues[1].trim()
                val stepText = unorderedMatch.groupValues[2].trim()
                
                val status = when (statusMarker) {
                    "✓", "x", "X" -> StepStatus.COMPLETED
                    "!" -> StepStatus.FAILED
                    "*" -> StepStatus.IN_PROGRESS
                    else -> StepStatus.TODO
                }
                
                // Extract file links from step text
                val fileLinks = extractFileLinks(stepText)
                
                currentSteps.add(
                    PlanStep(
                        text = stepText,
                        status = status,
                        fileLinks = fileLinks
                    )
                )
                continue
            }
            
            // If we're inside an item but not a step, append to last step
            if (currentItem != null && currentSteps.isNotEmpty() && trimmed.startsWith("-")) {
                val lastStep = currentSteps.last()
                currentSteps[currentSteps.size - 1] = lastStep.copy(
                    text = "${lastStep.text} ${trimmed.removePrefix("-").trim()}"
                )
            }
        }
        
        // Save last item
        currentItem?.let {
            items.add(it.copy(steps = currentSteps.toList()))
        }
        
        return items
    }
    
    private fun extractFileLinks(text: String): List<FileLink> {
        val links = mutableListOf<FileLink>()
        val matches = FILE_LINK_PATTERN.findAll(text)
        
        matches.forEach { match ->
            val displayText = match.groupValues[1]
            val filePath = match.groupValues[2]
            links.add(FileLink(displayText, filePath))
        }
        
        return links
    }
}

/**
 * Displays AI-generated modification plan with modern Plan-like UI
 */
@Composable
fun ModificationPlanSection(
    planOutput: String,
    isActive: Boolean,
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
                            PlanItemCard(item = item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanItemCard(item: PlanItem) {
    var isExpanded by remember { mutableStateOf(true) }
    
    val priorityColor = when {
        item.priority.contains("关键") || item.priority.contains("CRITICAL") -> AutoDevColors.Red.c600
        item.priority.contains("高") || item.priority.contains("HIGH") -> AutoDevColors.Amber.c600
        item.priority.contains("中等") || item.priority.contains("MEDIUM") -> AutoDevColors.Blue.c600
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Item header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isExpanded) AutoDevComposeIcons.ExpandMore else AutoDevComposeIcons.ChevronRight,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
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
                        text = item.priority,
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
                        PlanStepItem(step = step)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanStepItem(step: PlanStep) {
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
            // Step text with file links
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
                            
                            // Add link with style
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline,
                                    fontWeight = FontWeight.Medium
                                )
                            ) {
                                append(link.displayText)
                            }
                            
                            lastIndex = linkIndex + linkPattern.length
                        }
                    }
                    
                    // Add remaining text
                    if (lastIndex < remainingText.length) {
                        append(remainingText.substring(lastIndex))
                    }
                }
            }
            
            Text(
                text = annotatedText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.2
            )
        }
    }
}
