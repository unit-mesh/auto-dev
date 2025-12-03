package cc.unitmesh.devins.ui.compose.editor.plan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.agent.plan.AgentPlan
import cc.unitmesh.agent.plan.PlanStep
import cc.unitmesh.agent.plan.PlanTask
import cc.unitmesh.agent.plan.TaskStatus

/**
 * Plan Summary Bar Component
 *
 * Displays a collapsible summary of the current plan above the input box.
 * Shows progress, current step, and allows expanding to see full plan details.
 *
 * Similar to FileChangeSummary component pattern.
 */
@Composable
fun PlanSummaryBar(
    plan: AgentPlan?,
    modifier: Modifier = Modifier,
    onViewDetails: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    // Don't render if no plan
    if (plan == null || plan.tasks.isEmpty()) {
        return
    }

    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(topEnd = 4.dp, topStart = 4.dp, bottomEnd = 0.dp, bottomStart = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = when (plan.status) {
            TaskStatus.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            TaskStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Collapsed header
            PlanSummaryHeader(
                plan = plan,
                isExpanded = isExpanded,
                onExpandToggle = { isExpanded = !isExpanded },
                onDismiss = onDismiss
            )

            // Expanded content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    PlanExpandedContent(plan = plan)
                }
            }
        }
    }
}

@Composable
private fun PlanSummaryHeader(
    plan: AgentPlan,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onDismiss: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandToggle() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: icon, title, progress
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Status icon
            PlanStatusIcon(status = plan.status)

            // Expand arrow
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Title
            Text(
                text = plan.tasks.firstOrNull()?.title ?: "Plan",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            // Progress indicator
            PlanProgressBadge(plan = plan)
        }

        // Right side: current step and dismiss
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Current step description (compact)
            val currentStep = findCurrentStep(plan)
            if (currentStep != null && !isExpanded) {
                Text(
                    text = currentStep,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 150.dp)
                )
            }

            // Dismiss button
            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanStatusIcon(status: TaskStatus) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    when (status) {
        TaskStatus.COMPLETED -> Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Completed",
            modifier = Modifier.size(18.dp),
            tint = Color(0xFF4CAF50)
        )
        TaskStatus.FAILED -> Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Failed",
            modifier = Modifier.size(18.dp),
            tint = Color(0xFFF44336)
        )
        TaskStatus.IN_PROGRESS -> Icon(
            imageVector = Icons.Default.Sync,
            contentDescription = "In Progress",
            modifier = Modifier.size(18.dp).rotate(rotation),
            tint = Color(0xFF2196F3)
        )
        TaskStatus.BLOCKED -> Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Blocked",
            modifier = Modifier.size(18.dp),
            tint = Color(0xFFFF9800)
        )
        else -> Icon(
            imageVector = Icons.Default.Assignment,
            contentDescription = "Plan",
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun PlanProgressBadge(plan: AgentPlan) {
    val totalSteps = plan.tasks.sumOf { it.totalStepCount }
    val completedSteps = plan.tasks.sumOf { it.completedStepCount }
    val progress = plan.progressPercent

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Progress bar
        LinearProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier
                .width(60.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = when (plan.status) {
                TaskStatus.COMPLETED -> Color(0xFF4CAF50)
                TaskStatus.FAILED -> Color(0xFFF44336)
                else -> Color(0xFF2196F3)
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        // Progress text
        Text(
            text = "$completedSteps/$totalSteps",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun PlanExpandedContent(plan: AgentPlan) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(plan.tasks, key = { it.id }) { task ->
            TaskSummaryItem(task = task)
        }
    }
}

@Composable
private fun TaskSummaryItem(task: PlanTask) {
    var isExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                RoundedCornerShape(4.dp)
            )
            .padding(8.dp)
    ) {
        // Task header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                StepStatusIcon(status = task.status, size = 14)
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${task.completedStepCount}/${task.totalStepCount}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }

        // Steps (if expanded)
        AnimatedVisibility(visible = isExpanded && task.steps.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(start = 20.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                task.steps.forEach { step ->
                    StepItem(step = step)
                }
            }
        }
    }
}

@Composable
private fun StepItem(step: PlanStep) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        StepStatusIcon(status = step.status, size = 12)
        Text(
            text = step.description,
            style = MaterialTheme.typography.labelSmall,
            color = when (step.status) {
                TaskStatus.COMPLETED -> MaterialTheme.colorScheme.onSurfaceVariant
                TaskStatus.FAILED -> Color(0xFFF44336)
                else -> MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StepStatusIcon(status: TaskStatus, size: Int = 14) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    when (status) {
        TaskStatus.COMPLETED -> Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Completed",
            modifier = Modifier.size(size.dp),
            tint = Color(0xFF4CAF50)
        )
        TaskStatus.FAILED -> Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Failed",
            modifier = Modifier.size(size.dp),
            tint = Color(0xFFF44336)
        )
        TaskStatus.IN_PROGRESS -> Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "In Progress",
            modifier = Modifier.size(size.dp).rotate(rotation),
            tint = Color(0xFF2196F3)
        )
        else -> Box(
            modifier = Modifier
                .size((size - 4).dp)
                .background(Color(0xFF9E9E9E).copy(alpha = 0.3f), CircleShape)
        )
    }
}

/**
 * Find the current step description (first in-progress or first todo)
 */
private fun findCurrentStep(plan: AgentPlan): String? {
    for (task in plan.tasks) {
        for (step in task.steps) {
            if (step.status == TaskStatus.IN_PROGRESS) {
                return step.description
            }
        }
    }
    for (task in plan.tasks) {
        for (step in task.steps) {
            if (step.status == TaskStatus.TODO) {
                return step.description
            }
        }
    }
    return null
}

