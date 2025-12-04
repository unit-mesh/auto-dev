package cc.unitmesh.devins.idea.toolwindow.plan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.agent.plan.AgentPlan
import cc.unitmesh.agent.plan.PlanStep
import cc.unitmesh.agent.plan.PlanTask
import cc.unitmesh.agent.plan.TaskStatus
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys

/**
 * Plan Summary Bar for IntelliJ IDEA using Jewel components.
 *
 * Displays a collapsible summary of the current plan above the input box.
 * Uses Jewel theming and components for native IntelliJ look and feel.
 */
private val planSummaryLogger = com.intellij.openapi.diagnostic.Logger.getInstance("IdeaPlanSummaryBar")

@Composable
fun IdeaPlanSummaryBar(
    plan: AgentPlan?,
    modifier: Modifier = Modifier,
    onViewDetails: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    // Log for debugging
    planSummaryLogger.info("IdeaPlanSummaryBar called: plan=${plan != null}, tasks=${plan?.tasks?.size ?: 0}")

    // Don't render if no plan
    if (plan == null || plan.tasks.isEmpty()) {
        planSummaryLogger.info("IdeaPlanSummaryBar: not rendering (plan is null or empty)")
        return
    }

    var isExpanded by remember { mutableStateOf(false) }

    val backgroundColor = when (plan.status) {
        TaskStatus.FAILED -> AutoDevColors.Red.c900.copy(alpha = 0.2f)
        TaskStatus.COMPLETED -> AutoDevColors.Green.c900.copy(alpha = 0.2f)
        else -> JewelTheme.globalColors.panelBackground
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
    ) {
        // Collapsed header
        IdeaPlanSummaryHeader(
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(JewelTheme.globalColors.borders.normal)
                )
                IdeaPlanExpandedContent(plan = plan)
            }
        }
    }
}

@Composable
private fun IdeaPlanSummaryHeader(
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
            IdeaPlanStatusIcon(status = plan.status)

            // Expand arrow
            Icon(
                key = if (isExpanded) AllIconsKeys.General.ArrowDown else AllIconsKeys.General.ArrowRight,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                modifier = Modifier.size(12.dp),
                tint = AutoDevColors.Neutral.c400
            )

            // Title
            Text(
                text = plan.tasks.firstOrNull()?.title ?: "Plan",
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            // Progress indicator
            IdeaPlanProgressBadge(plan = plan)
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
                    style = JewelTheme.defaultTextStyle.copy(
                        fontSize = 10.sp,
                        color = AutoDevColors.Neutral.c400
                    ),
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
                        key = AllIconsKeys.Actions.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(12.dp),
                        tint = AutoDevColors.Neutral.c400
                    )
                }
            }
        }
    }
}

@Composable
private fun IdeaPlanStatusIcon(status: TaskStatus) {
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
            key = AllIconsKeys.Actions.Checked,
            contentDescription = "Completed",
            modifier = Modifier.size(16.dp),
            tint = AutoDevColors.Green.c400
        )
        TaskStatus.FAILED -> Icon(
            key = AllIconsKeys.General.Error,
            contentDescription = "Failed",
            modifier = Modifier.size(16.dp),
            tint = AutoDevColors.Red.c400
        )
        TaskStatus.IN_PROGRESS -> Icon(
            key = AllIconsKeys.Actions.Refresh,
            contentDescription = "In Progress",
            modifier = Modifier.size(16.dp).rotate(rotation),
            tint = AutoDevColors.Blue.c400
        )
        TaskStatus.BLOCKED -> Icon(
            key = AllIconsKeys.General.Warning,
            contentDescription = "Blocked",
            modifier = Modifier.size(16.dp),
            tint = AutoDevColors.Amber.c400
        )
        else -> Icon(
            key = AllIconsKeys.General.TodoDefault,
            contentDescription = "Plan",
            modifier = Modifier.size(16.dp),
            tint = AutoDevColors.Neutral.c400
        )
    }
}

@Composable
private fun IdeaPlanProgressBadge(plan: AgentPlan) {
    val totalSteps = plan.tasks.sumOf { it.totalStepCount }
    val completedSteps = plan.tasks.sumOf { it.completedStepCount }
    val progress = plan.progressPercent

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Progress bar (simple Box-based implementation)
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(AutoDevColors.Neutral.c700)
        ) {
            val progressColor = when (plan.status) {
                TaskStatus.COMPLETED -> AutoDevColors.Green.c400
                TaskStatus.FAILED -> AutoDevColors.Red.c400
                else -> AutoDevColors.Blue.c400
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = progress / 100f)
                    .background(progressColor)
            )
        }

        // Progress text
        Text(
            text = "$completedSteps/$totalSteps",
            style = JewelTheme.defaultTextStyle.copy(
                fontSize = 10.sp,
                color = AutoDevColors.Neutral.c400
            )
        )
    }
}

@Composable
private fun IdeaPlanExpandedContent(plan: AgentPlan) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(plan.tasks, key = { it.id }) { task ->
            IdeaTaskSummaryItem(task = task)
        }
    }
}

@Composable
private fun IdeaTaskSummaryItem(task: PlanTask) {
    var isExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                JewelTheme.globalColors.panelBackground.copy(alpha = 0.5f),
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
                IdeaStepStatusIcon(status = task.status, size = 14)
                Text(
                    text = task.title,
                    style = JewelTheme.defaultTextStyle.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${task.completedStepCount}/${task.totalStepCount}",
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 10.sp,
                    color = AutoDevColors.Neutral.c400
                )
            )
        }

        // Steps (if expanded)
        AnimatedVisibility(visible = isExpanded && task.steps.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(start = 20.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                task.steps.forEach { step ->
                    IdeaStepItem(step = step)
                }
            }
        }
    }
}

@Composable
private fun IdeaStepItem(step: PlanStep) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        IdeaStepStatusIcon(status = step.status, size = 12)
        Text(
            text = step.description,
            style = JewelTheme.defaultTextStyle.copy(
                fontSize = 10.sp,
                color = when (step.status) {
                    TaskStatus.COMPLETED -> AutoDevColors.Neutral.c500
                    TaskStatus.FAILED -> AutoDevColors.Red.c400
                    else -> AutoDevColors.Neutral.c200
                }
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun IdeaStepStatusIcon(status: TaskStatus, size: Int = 14) {
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
            key = AllIconsKeys.Actions.Checked,
            contentDescription = "Completed",
            modifier = Modifier.size(size.dp),
            tint = AutoDevColors.Green.c400
        )
        TaskStatus.FAILED -> Icon(
            key = AllIconsKeys.Actions.Close,
            contentDescription = "Failed",
            modifier = Modifier.size(size.dp),
            tint = AutoDevColors.Red.c400
        )
        TaskStatus.IN_PROGRESS -> Icon(
            key = AllIconsKeys.Actions.Refresh,
            contentDescription = "In Progress",
            modifier = Modifier.size(size.dp).rotate(rotation),
            tint = AutoDevColors.Blue.c400
        )
        else -> Box(
            modifier = Modifier
                .size((size - 4).dp)
                .background(AutoDevColors.Neutral.c600.copy(alpha = 0.3f), CircleShape)
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

