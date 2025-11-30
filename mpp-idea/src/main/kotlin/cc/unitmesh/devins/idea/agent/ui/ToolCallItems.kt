package cc.unitmesh.devins.idea.agent.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.idea.agent.ToolCallInfo
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text

@Composable
fun ToolCallItem(
    toolName: String,
    details: String?,
    success: Boolean? = null,
    summary: String? = null,
    output: String? = null,
    executionTimeMs: Long? = null,
    onExpand: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(success == false) }
    LaunchedEffect(expanded) { if (expanded) onExpand() }
    val isExecuting = success == null

    Box(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
        .background(JewelTheme.globalColors.panelBackground).padding(8.dp)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(
                    indication = null, interactionSource = remember { MutableInteractionSource() }
                ) { if (details != null || output != null) expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = when { isExecuting -> "▶"; success == true -> "✓"; else -> "✗" },
                    style = JewelTheme.defaultTextStyle.copy(
                        color = when { isExecuting -> JewelTheme.globalColors.text.info
                            success == true -> JewelTheme.globalColors.text.info
                            else -> JewelTheme.globalColors.text.error }))
                Text(text = toolName, style = JewelTheme.defaultTextStyle.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f))
                if (summary != null) {
                    Text(text = "→ $summary", style = JewelTheme.defaultTextStyle.copy(
                        color = when (success) { true -> JewelTheme.globalColors.text.info
                            false -> JewelTheme.globalColors.text.error
                            else -> JewelTheme.globalColors.text.normal }))
                }
                if (executionTimeMs != null && executionTimeMs > 0) {
                    Text(text = "${executionTimeMs}ms", style = JewelTheme.defaultTextStyle.copy(
                        fontSize = 10.sp, color = JewelTheme.globalColors.text.info))
                }
                if (details != null || output != null) {
                    Text(text = if (expanded) "▲" else "▼",
                        style = JewelTheme.defaultTextStyle.copy(color = JewelTheme.globalColors.text.info))
                }
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()) {
                Column {
                    if (details != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Parameters:", style = JewelTheme.defaultTextStyle.copy(fontWeight = FontWeight.Medium))
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                            .background(JewelTheme.globalColors.panelBackground).padding(8.dp)) {
                            Text(text = details, style = JewelTheme.defaultTextStyle.copy(fontFamily = FontFamily.Monospace))
                        }
                    }
                    if (output != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Output:", style = JewelTheme.defaultTextStyle.copy(fontWeight = FontWeight.Medium))
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                            .background(JewelTheme.globalColors.panelBackground).padding(8.dp)) {
                            Text(text = output, style = JewelTheme.defaultTextStyle.copy(fontFamily = FontFamily.Monospace))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentToolCallItem(toolCall: ToolCallInfo, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
        .background(JewelTheme.globalColors.panelBackground).padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "⏳", style = JewelTheme.defaultTextStyle)
            Text(text = toolCall.toolName, style = JewelTheme.defaultTextStyle.copy(fontWeight = FontWeight.Bold))
            Text(text = toolCall.description, style = JewelTheme.defaultTextStyle.copy(color = JewelTheme.globalColors.text.info))
        }
    }
}

@Composable
fun TerminalOutputItem(
    command: String, output: String, exitCode: Int, executionTimeMs: Long,
    onExpand: () -> Unit = {}, modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(exitCode != 0) }
    LaunchedEffect(expanded) { if (expanded) onExpand() }
    val isSuccess = exitCode == 0

    Box(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
        .background(JewelTheme.globalColors.panelBackground).padding(8.dp)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(
                    indication = null, interactionSource = remember { MutableInteractionSource() }
                ) { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = if (isSuccess) "✓" else "✗",
                    style = JewelTheme.defaultTextStyle.copy(
                        color = if (isSuccess) JewelTheme.globalColors.text.info else JewelTheme.globalColors.text.error))
                Text(text = "Exit $exitCode",
                    style = JewelTheme.defaultTextStyle.copy(fontWeight = FontWeight.Medium,
                        color = if (isSuccess) JewelTheme.globalColors.text.info else JewelTheme.globalColors.text.error),
                    modifier = Modifier.weight(1f))
                Text(text = "${executionTimeMs}ms",
                    style = JewelTheme.defaultTextStyle.copy(fontSize = 10.sp, color = JewelTheme.globalColors.text.info))
                Text(text = if (expanded) "▲" else "▼",
                    style = JewelTheme.defaultTextStyle.copy(color = JewelTheme.globalColors.text.info))
            }
            AnimatedVisibility(visible = expanded && output.isNotEmpty(),
                enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    .clip(RoundedCornerShape(4.dp)).background(JewelTheme.globalColors.panelBackground).padding(8.dp)) {
                    Text(text = output, style = JewelTheme.defaultTextStyle.copy(fontFamily = FontFamily.Monospace))
                }
            }
        }
    }
}

