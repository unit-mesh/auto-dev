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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.compose.sketch.SketchRenderer
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors

/**
 * Represents a single modification plan item
 */
data class PlanItem(
    val number: Int,
    val title: String,
    val priority: String,
    val what: String,
    val why: String,
    val how: String
)

/**
 * Parses modification plan markdown into structured plan items
 */
object PlanParser {
    fun parse(planOutput: String): List<PlanItem> {
        val items = mutableListOf<PlanItem>()
        val lines = planOutput.lines()
        
        var currentItem: MutableMap<String, String>? = null
        var currentNumber = 0
        var currentField: String? = null
        
        for (line in lines) {
            // Match: ### 1. Title - Priority
            val headerMatch = Regex("""^###\s+(\d+)\.\s+(.+?)\s+-\s+(.+)$""").find(line.trim())
            if (headerMatch != null) {
                // Save previous item if exists
                currentItem?.let {
                    items.add(
                        PlanItem(
                            number = currentNumber,
                            title = it["title"] ?: "",
                            priority = it["priority"] ?: "",
                            what = it["what"] ?: "",
                            why = it["why"] ?: "",
                            how = it["how"] ?: ""
                        )
                    )
                }
                
                // Start new item
                currentNumber = headerMatch.groupValues[1].toIntOrNull() ?: 0
                currentItem = mutableMapOf(
                    "title" to headerMatch.groupValues[2].trim(),
                    "priority" to headerMatch.groupValues[3].trim()
                )
                currentField = null
                continue
            }
            
            // Match field headers
            when {
                line.trim().startsWith("**需要改什么**:") || line.trim().startsWith("**What**:") -> {
                    currentField = "what"
                    val content = line.substringAfter(":").trim()
                    if (content.isNotEmpty()) {
                        currentItem?.put("what", content)
                    }
                }
                line.trim().startsWith("**为什么改**:") || line.trim().startsWith("**Why**:") -> {
                    currentField = "why"
                    val content = line.substringAfter(":").trim()
                    if (content.isNotEmpty()) {
                        currentItem?.put("why", content)
                    }
                }
                line.trim().startsWith("**怎么改**:") || line.trim().startsWith("**How**:") -> {
                    currentField = "how"
                    val content = line.substringAfter(":").trim()
                    if (content.isNotEmpty()) {
                        currentItem?.put("how", content)
                    }
                }
                else -> {
                    // Append to current field if not empty
                    if (currentField != null && line.trim().isNotEmpty() && !line.trim().startsWith("#")) {
                        val existing = currentItem?.get(currentField) ?: ""
                        currentItem?.put(currentField, if (existing.isEmpty()) line.trim() else "$existing ${line.trim()}")
                    }
                }
            }
        }
        
        // Save last item
        currentItem?.let {
            items.add(
                PlanItem(
                    number = currentNumber,
                    title = it["title"] ?: "",
                    priority = it["priority"] ?: "",
                    what = it["what"] ?: "",
                    why = it["why"] ?: "",
                    how = it["how"] ?: ""
                )
            )
        }
        
        return items
    }
}

/**
 * Displays AI-generated modification plan with structured, collapsible cards
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
        shape = RoundedCornerShape(6.dp)
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
                        modifier = Modifier.size(20.dp)
                    )

                    Icon(
                        imageVector = AutoDevComposeIcons.Article,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )

                    Text(
                        text = "修改计划 (AI)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (planItems.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${planItems.size} 项",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
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
                        .padding(horizontal = 12.dp, vertical = 0.dp)
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
        else -> AutoDevColors.Blue.c600
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Item header
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
                    color = priorityColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = item.priority,
                        style = MaterialTheme.typography.labelSmall,
                        color = priorityColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Item details
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
                    if (item.what.isNotEmpty()) {
                        PlanField(label = "需要改什么", content = item.what)
                    }
                    if (item.why.isNotEmpty()) {
                        PlanField(label = "为什么改", content = item.why)
                    }
                    if (item.how.isNotEmpty()) {
                        PlanField(label = "怎么改", content = item.how)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanField(label: String, content: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
