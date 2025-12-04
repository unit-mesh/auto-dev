package cc.unitmesh.devins.idea.renderer.sketch

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.idea.renderer.sketch.actions.ExecutionResult
import cc.unitmesh.devins.idea.renderer.sketch.actions.IdeaTerminalActions
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import com.intellij.openapi.project.Project
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.icons.AllIconsKeys

/**
 * Terminal renderer for IntelliJ IDEA with Jewel styling.
 */
@Composable
fun IdeaTerminalRenderer(
    command: String,
    project: Project? = null,
    isComplete: Boolean = false,
    modifier: Modifier = Modifier
) {
    var executionState by remember { mutableStateOf(TerminalState.IDLE) }
    var executionResult by remember { mutableStateOf<ExecutionResult?>(null) }
    var showOutput by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    
    // Check if command is dangerous
    val (isDangerous, dangerReason) = remember(command) { 
        IdeaTerminalActions.checkDangerousCommand(command) 
    }
    
    Column(
        modifier = modifier
            .background(JewelTheme.globalColors.panelBackground)
            .clip(RoundedCornerShape(4.dp))
    ) {
        // Toolbar
        TerminalToolbar(
            command = command,
            project = project,
            executionState = executionState,
            isDangerous = isDangerous,
            copied = copied,
            onExecute = {
                if (project != null && !isDangerous) {
                    executionState = TerminalState.RUNNING
                    AutoDevCoroutineScope.scope(project).launch {
                        val result = IdeaTerminalActions.executeCommand(project, command)
                        executionResult = result
                        executionState = if (result.isSuccess) TerminalState.SUCCESS else TerminalState.FAILED
                        showOutput = true
                    }
                }
            },
            onCopy = { 
                if (IdeaTerminalActions.copyToClipboard(command)) copied = true 
            },
            onToggleOutput = { showOutput = !showOutput }
        )
        
        // Command display
        CommandDisplay(command = command, isDangerous = isDangerous, dangerReason = dangerReason)
        // Output (if available)
//        if (showOutput && executionResult != null) {
//            OutputDisplay(result = executionResult!!)
//        }
    }
}

private enum class TerminalState { IDLE, RUNNING, SUCCESS, FAILED }

@Composable
private fun TerminalToolbar(
    command: String, project: Project?, executionState: TerminalState, isDangerous: Boolean,
    copied: Boolean, onExecute: () -> Unit, onCopy: () -> Unit, onToggleOutput: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(JewelTheme.globalColors.panelBackground)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(AllIconsKeys.Debugger.Console, "Terminal", Modifier.size(14.dp), tint = AutoDevColors.Neutral.c400)
            Text("Terminal", style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold))
            // Status indicator
            when (executionState) {
                TerminalState.RUNNING -> CircularProgressIndicator(Modifier.size(14.dp))
                TerminalState.SUCCESS -> Icon(AllIconsKeys.Actions.Checked, "Success", Modifier.size(14.dp), tint = AutoDevColors.Green.c400)
                TerminalState.FAILED -> Icon(AllIconsKeys.General.Error, "Failed", Modifier.size(14.dp), tint = AutoDevColors.Red.c400)
                else -> {}
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (project != null && !isDangerous) {
                TerminalActionButton(
                    if (executionState == TerminalState.RUNNING) "Running..." else "Execute",
                    if (executionState == TerminalState.RUNNING) AllIconsKeys.Actions.Suspend else AllIconsKeys.Actions.Execute,
                    enabled = executionState != TerminalState.RUNNING, onClick = onExecute
                )
            }
            TerminalActionButton(if (copied) "Copied!" else "Copy",
                if (copied) AllIconsKeys.Actions.Checked else AllIconsKeys.Actions.Copy, onClick = onCopy)
            TerminalActionButton("Toggle Output", AllIconsKeys.Actions.PreviewDetails, onClick = onToggleOutput)
        }
    }
}

@Composable
private fun TerminalActionButton(tooltip: String, iconKey: org.jetbrains.jewel.ui.icon.IconKey, 
                                  enabled: Boolean = true, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    Tooltip(tooltip = { Text(tooltip) }) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(24.dp).hoverable(interactionSource)
            .background(if (isHovered && enabled) AutoDevColors.Neutral.c700.copy(alpha = 0.3f) else Color.Transparent)) {
            Icon(iconKey, tooltip, Modifier.size(16.dp), 
                tint = if (enabled) AutoDevColors.Neutral.c300 else AutoDevColors.Neutral.c600)
        }
    }
}

@Composable
private fun CommandDisplay(command: String, isDangerous: Boolean, dangerReason: String) {
    Column(Modifier.fillMaxWidth().padding(8.dp)) {
        if (isDangerous) {
            Row(Modifier.fillMaxWidth().background(AutoDevColors.Red.c900.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                .padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(AllIconsKeys.General.Warning, "Warning", Modifier.size(16.dp), tint = AutoDevColors.Red.c400)
                Text("Dangerous command blocked: $dangerReason", 
                    style = JewelTheme.defaultTextStyle.copy(fontSize = 11.sp, color = AutoDevColors.Red.c300))
            }
            Spacer(Modifier.height(8.dp))
        }
        Text(command, style = JewelTheme.defaultTextStyle.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
            modifier = Modifier.fillMaxWidth().background(AutoDevColors.Neutral.c900, RoundedCornerShape(4.dp)).padding(8.dp))
    }
}

@Composable
private fun OutputDisplay(result: ExecutionResult) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 8.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Output (Exit: ${result.exitCode})", style = JewelTheme.defaultTextStyle.copy(fontSize = 10.sp, 
                color = if (result.isSuccess) AutoDevColors.Green.c400 else AutoDevColors.Red.c400))
        }
        Spacer(Modifier.height(4.dp))
        Text(result.displayOutput.ifBlank { "(no output)" }, 
            style = JewelTheme.defaultTextStyle.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = AutoDevColors.Neutral.c300),
            modifier = Modifier.fillMaxWidth().background(AutoDevColors.Neutral.c800, RoundedCornerShape(4.dp)).padding(8.dp))
    }
}

