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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.agent.diff.DiffLineType
import cc.unitmesh.agent.diff.DiffParser
import cc.unitmesh.devins.idea.renderer.sketch.actions.IdeaDiffActions
import cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import com.intellij.openapi.project.Project
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip
import org.jetbrains.jewel.ui.icons.AllIconsKeys

/**
 * Diff renderer for IntelliJ IDEA with Jewel styling.
 * Renders unified diff format with syntax highlighting and action buttons.
 * 
 * Related GitHub Issue: https://github.com/phodal/auto-dev/issues/25
 */
@Composable
fun IdeaDiffRenderer(
    diffContent: String,
    project: Project? = null,
    modifier: Modifier = Modifier
) {
    val fileDiffs = remember(diffContent) { DiffParser.parse(diffContent) }
    var isRepairing by remember { mutableStateOf(false) }
    var patchApplied by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Toolbar with action buttons
        if (project != null && fileDiffs.isNotEmpty()) {
            DiffToolbar(
                diffContent = diffContent,
                project = project,
                isRepairing = isRepairing,
                patchApplied = patchApplied,
                onAccept = {
                    val success = IdeaDiffActions.acceptPatch(project, diffContent)
                    if (success) patchApplied = true
                },
                onReject = {
                    IdeaDiffActions.rejectPatch(project)
                    patchApplied = false
                },
                onViewDiff = {
                    IdeaDiffActions.viewDiff(project, diffContent) {
                        val success = IdeaDiffActions.acceptPatch(project, diffContent)
                        if (success) patchApplied = true
                    }
                },
                onRepair = {
                    isRepairing = true
                    IdeaDiffActions.repairPatch(project, diffContent) {
                        isRepairing = false
                    }
                }
            )
        }

        if (fileDiffs.isEmpty()) {
            // Show error with repair option
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Unable to parse diff content",
                    style = JewelTheme.defaultTextStyle.copy(
                        fontSize = 11.sp,
                        color = AutoDevColors.Red.c400
                    )
                )
                if (project != null && !isRepairing) {
                    DiffActionButton(
                        tooltip = "Repair with AI",
                        onClick = {
                            isRepairing = true
                            IdeaDiffActions.repairPatch(project, diffContent) {
                                isRepairing = false
                            }
                        }
                    ) {
                        Icon(
                            key = AllIconsKeys.Actions.IntentionBulb,
                            contentDescription = "Repair",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                if (isRepairing) {
                    Text(
                        text = "Repairing...",
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 10.sp,
                            color = AutoDevColors.Blue.c400
                        )
                    )
                }
            }
            return@Column
        }

        fileDiffs.forEach { fileDiff ->
            // File header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .background(JewelTheme.globalColors.panelBackground)
                    .padding(8.dp)
            ) {
                val displayPath = fileDiff.newPath?.takeIf { it.isNotBlank() }
                    ?: fileDiff.oldPath
                    ?: "unknown"
                Text(
                    text = displayPath,
                    style = JewelTheme.defaultTextStyle.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AutoDevColors.Blue.c400
                    )
                )
            }

            // Diff hunks
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                    .background(JewelTheme.globalColors.panelBackground.copy(alpha = 0.3f))
            ) {
                fileDiff.hunks.forEach { hunk ->
                    // Hunk header
                    Text(
                        text = "@@ -${hunk.oldStartLine},${hunk.oldLineCount} +${hunk.newStartLine},${hunk.newLineCount} @@",
                        style = JewelTheme.defaultTextStyle.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = AutoDevColors.Blue.c300
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    // Diff lines
                    hunk.lines.forEach { diffLine ->
                        val (bgColor, textColor) = when (diffLine.type) {
                            DiffLineType.ADDED -> Pair(
                                AutoDevColors.Diff.Dark.addedBg,
                                AutoDevColors.Green.c400
                            )
                            DiffLineType.DELETED -> Pair(
                                AutoDevColors.Diff.Dark.deletedBg,
                                AutoDevColors.Red.c400
                            )
                            else -> Pair(
                                Color.Transparent,
                                JewelTheme.globalColors.text.normal
                            )
                        }

                        Text(
                            text = diffLine.content,
                            style = JewelTheme.defaultTextStyle.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = textColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bgColor)
                                .padding(horizontal = 8.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DiffToolbar(
    diffContent: String,
    project: Project,
    isRepairing: Boolean,
    patchApplied: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onViewDiff: () -> Unit,
    onRepair: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(JewelTheme.globalColors.panelBackground)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Accept button
        DiffActionButton(
            tooltip = "Accept and apply patch",
            onClick = onAccept,
            enabled = !patchApplied
        ) {
            Icon(
                key = AllIconsKeys.Actions.Commit,
                contentDescription = "Accept",
                modifier = Modifier.size(14.dp),
                tint = if (patchApplied) AutoDevColors.Neutral.c500 else AutoDevColors.Green.c400
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Reject/Undo button
        DiffActionButton(
            tooltip = "Reject/Undo patch",
            onClick = onReject
        ) {
            Icon(
                key = AllIconsKeys.Actions.Rollback,
                contentDescription = "Reject",
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // View Diff button
        DiffActionButton(
            tooltip = "View diff in dialog",
            onClick = onViewDiff
        ) {
            Icon(
                key = AllIconsKeys.Actions.ListChanges,
                contentDescription = "View Diff",
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Repair button
        DiffActionButton(
            tooltip = "Repair patch with AI",
            onClick = onRepair,
            enabled = !isRepairing
        ) {
            if (isRepairing) {
                Icon(
                    imageVector = IdeaComposeIcons.Refresh,
                    contentDescription = "Repairing...",
                    modifier = Modifier.size(14.dp),
                    tint = AutoDevColors.Blue.c400
                )
            } else {
                Icon(
                    key = AllIconsKeys.Actions.IntentionBulb,
                    contentDescription = "Repair",
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun DiffActionButton(
    tooltip: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Tooltip(tooltip = { Text(tooltip) }) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .hoverable(interactionSource = interactionSource)
                .background(
                    if (isHovered && enabled) JewelTheme.globalColors.borders.normal.copy(alpha = 0.3f)
                    else Color.Transparent
                )
                .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
