package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cc.unitmesh.agent.plan.AgentPlan
import cc.unitmesh.agent.plan.PlanStep
import cc.unitmesh.agent.plan.PlanTask
import cc.unitmesh.agent.plan.TaskStatus

val TaskStatus.planColor: Color
    get() = when (this) {
        TaskStatus.TODO -> Color(0xFF9E9E9E)
        TaskStatus.IN_PROGRESS -> Color(0xFF2196F3)
        TaskStatus.COMPLETED -> Color(0xFF4CAF50)
        TaskStatus.FAILED -> Color(0xFFF44336)
        TaskStatus.BLOCKED -> Color(0xFFFF9800)
    }

@Composable
fun TaskStatus.planIcon(): Unit = when (this) {
    TaskStatus.TODO -> Icon(Icons.Default.RadioButtonUnchecked, null, tint = planColor)
    TaskStatus.IN_PROGRESS -> Icon(Icons.Default.Refresh, null, tint = planColor)
    TaskStatus.COMPLETED -> Icon(Icons.Default.CheckCircle, null, tint = planColor)
    TaskStatus.FAILED -> Icon(Icons.Default.Error, null, tint = planColor)
    TaskStatus.BLOCKED -> Icon(Icons.Default.Warning, null, tint = planColor)
}

@Composable
fun PlanPanel(
    plan: AgentPlan?,
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    onStepClick: ((taskId: String, stepId: String) -> Unit)? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            PlanPanelHeader(plan = plan, onClose = onClose)
            HorizontalDivider()
            if (plan == null || plan.tasks.isEmpty()) {
                EmptyPlanContent()
            } else {
                PlanContent(plan = plan, onStepClick = onStepClick)
            }
        }
    }
}

@Composable
private fun PlanPanelHeader(plan: AgentPlan?, onClose: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Assignment, contentDescription = "Plan", tint = MaterialTheme.colorScheme.primary)
                Text("Plan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (plan != null) {
                    Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) { Text("${plan.progressPercent}%") }
                }
            }
            IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun EmptyPlanContent() {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Assignment, null, Modifier.size(48.dp), MaterialTheme.colorScheme.outline)
            Text("No active plan", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PlanContent(plan: AgentPlan, onStepClick: ((taskId: String, stepId: String) -> Unit)?) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(plan.tasks, key = { it.id }) { task -> PlanTaskCard(task = task, onStepClick = onStepClick) }
    }
}

@Composable
private fun PlanTaskCard(task: PlanTask, onStepClick: ((taskId: String, stepId: String) -> Unit)?) {
    var expanded by remember { mutableStateOf(true) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = task.status.planColor.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(modifier = Modifier.size(20.dp)) { task.status.planIcon() }
                    Text(
                        task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${task.completedStepCount}/${task.totalStepCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        if (expanded) "Collapse" else "Expand",
                        Modifier.size(20.dp),
                        MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            AnimatedVisibility(visible = expanded && task.steps.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = 28.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    task.steps.forEach { step ->
                        PlanStepItem(step = step, onClick = { onStepClick?.invoke(task.id, step.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanStepItem(step: PlanStep, onClick: (() -> Unit)?) {
    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart)
    )
    Row(
        modifier = Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) {
            when (step.status) {
                TaskStatus.IN_PROGRESS -> Icon(Icons.Default.Refresh, null, Modifier.size(14.dp).rotate(angle), step.status.planColor)
                TaskStatus.COMPLETED -> Icon(Icons.Default.Check, null, Modifier.size(14.dp), step.status.planColor)
                TaskStatus.FAILED -> Icon(Icons.Default.Close, null, Modifier.size(14.dp), step.status.planColor)
                else -> Box(modifier = Modifier.size(10.dp).background(step.status.planColor.copy(alpha = 0.3f), CircleShape))
            }
        }
        Text(
            step.description,
            style = MaterialTheme.typography.bodySmall,
            color = if (step.status == TaskStatus.COMPLETED) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (step.status == TaskStatus.COMPLETED) TextDecoration.LineThrough else TextDecoration.None,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

