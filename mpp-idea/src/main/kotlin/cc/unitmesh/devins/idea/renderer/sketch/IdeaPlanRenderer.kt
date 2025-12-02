package cc.unitmesh.devins.idea.renderer.sketch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.idea.renderer.sketch.actions.IdeaPlanActions
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import cc.unitmesh.devti.observer.plan.AgentTaskEntry
import cc.unitmesh.devti.observer.plan.TaskStatus
import com.intellij.openapi.project.Project
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip
import org.jetbrains.jewel.ui.icons.AllIconsKeys

/**
 * Plan renderer for IntelliJ IDEA with Jewel styling.
 */
@Composable
fun IdeaPlanRenderer(
    planContent: String,
    project: Project? = null,
    isComplete: Boolean = false,
    modifier: Modifier = Modifier
) {
    val planItems = remember(planContent) { IdeaPlanActions.parsePlan(planContent) }
    var isCompressed by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .background(JewelTheme.globalColors.panelBackground)
            .clip(RoundedCornerShape(4.dp))
    ) {
        PlanToolbar(planContent, project, isCompressed, copied,
            onToggleCompress = { isCompressed = !isCompressed },
            onCopy = { if (IdeaPlanActions.copyToClipboard(planContent)) copied = true },
            onPin = { project?.let { IdeaPlanActions.pinToToolWindow(it, planContent) } }
        )
        
        if (!isCompressed) {
            Column(modifier = Modifier.padding(8.dp)) {
                planItems.forEachIndexed { index, entry ->
                    PlanSection(index, entry, Modifier.fillMaxWidth())
                    if (index < planItems.lastIndex) Spacer(modifier = Modifier.height(8.dp))
                }
            }
        } else {
            CompressedPlanView(planItems)
        }
    }
}

@Composable
private fun PlanToolbar(
    planContent: String, project: Project?, isCompressed: Boolean, copied: Boolean,
    onToggleCompress: () -> Unit, onCopy: () -> Unit, onPin: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(JewelTheme.globalColors.panelBackground)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Plan", style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            PlanActionButton(if (isCompressed) "Expand" else "Compress",
                if (isCompressed) AllIconsKeys.Actions.Expandall else AllIconsKeys.Actions.Collapseall, onToggleCompress)
            PlanActionButton(if (copied) "Copied!" else "Copy Plan",
                if (copied) AllIconsKeys.Actions.Checked else AllIconsKeys.Actions.Copy, onCopy)
            if (project != null) PlanActionButton("Pin to Planner", AllIconsKeys.Actions.PinTab, onPin)
        }
    }
}

@Composable
private fun PlanActionButton(tooltip: String, iconKey: org.jetbrains.jewel.ui.icon.IconKey, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    Tooltip(tooltip = { Text(tooltip) }) {
        IconButton(onClick = onClick, modifier = Modifier.size(24.dp).hoverable(interactionSource)
            .background(if (isHovered) AutoDevColors.Neutral.c700.copy(alpha = 0.3f) else Color.Transparent)) {
            Icon(iconKey, tooltip, Modifier.size(16.dp), tint = AutoDevColors.Neutral.c300)
        }
    }
}

@Composable
private fun CompressedPlanView(planItems: List<AgentTaskEntry>) {
    val completedCount = planItems.count { it.status == TaskStatus.COMPLETED }
    Row(Modifier.fillMaxWidth().padding(8.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text("${planItems.size} sections", style = JewelTheme.defaultTextStyle.copy(fontSize = 11.sp))
        Text("$completedCount/${planItems.size} completed", style = JewelTheme.defaultTextStyle.copy(
            fontSize = 11.sp, color = if (completedCount == planItems.size) AutoDevColors.Green.c400 else AutoDevColors.Neutral.c400))
    }
}

@Composable
private fun PlanSection(index: Int, entry: AgentTaskEntry, modifier: Modifier = Modifier) {
    var isExpanded by remember { mutableStateOf(true) }
    Column(modifier.background(JewelTheme.globalColors.panelBackground.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(8.dp)) {
        Row(Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded }, Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (isExpanded) AllIconsKeys.General.ArrowDown else AllIconsKeys.General.ArrowRight,
                    if (isExpanded) "Collapse" else "Expand", Modifier.size(12.dp), tint = AutoDevColors.Neutral.c400)
                StatusIcon(entry.status)
                Text("${index + 1}. ${entry.title}", style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            }
            StatusLabel(entry.status)
        }
        if (isExpanded && entry.steps.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Column(Modifier.padding(start = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                entry.steps.forEach { step -> PlanStep(step) }
            }
        }
    }
}

@Composable
private fun PlanStep(step: cc.unitmesh.devti.observer.plan.AgentPlanStep) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (step.completed) AllIconsKeys.Actions.Checked else AllIconsKeys.Nodes.EmptyNode,
            if (step.completed) "Completed" else "Pending", Modifier.size(14.dp),
            tint = if (step.completed) AutoDevColors.Green.c400 else AutoDevColors.Neutral.c500)
        Text(step.step, style = JewelTheme.defaultTextStyle.copy(fontSize = 11.sp,
            color = if (step.completed) AutoDevColors.Neutral.c400 else AutoDevColors.Neutral.c200))
    }
}

@Composable
private fun StatusIcon(status: TaskStatus) {
    val (iconKey, tint) = when (status) {
        TaskStatus.COMPLETED -> AllIconsKeys.Actions.Checked to AutoDevColors.Green.c400
        TaskStatus.FAILED -> AllIconsKeys.General.Error to AutoDevColors.Red.c400
        TaskStatus.IN_PROGRESS -> AllIconsKeys.Actions.Execute to AutoDevColors.Blue.c400
        TaskStatus.TODO -> AllIconsKeys.General.TodoDefault to AutoDevColors.Neutral.c500
    }
    Icon(iconKey, status.name, Modifier.size(14.dp), tint = tint)
}

@Composable
private fun StatusLabel(status: TaskStatus) {
    val (text, color) = when (status) {
        TaskStatus.COMPLETED -> "Done" to AutoDevColors.Green.c400
        TaskStatus.FAILED -> "Failed" to AutoDevColors.Red.c400
        TaskStatus.IN_PROGRESS -> "Running" to AutoDevColors.Blue.c400
        TaskStatus.TODO -> "Todo" to AutoDevColors.Neutral.c500
    }
    Text(text, style = JewelTheme.defaultTextStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color))
}

