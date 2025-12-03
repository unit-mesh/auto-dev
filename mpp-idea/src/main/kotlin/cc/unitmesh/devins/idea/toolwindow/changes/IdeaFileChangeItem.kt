package cc.unitmesh.devins.idea.toolwindow.changes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.agent.diff.ChangeType
import cc.unitmesh.agent.diff.FileChange
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys

/**
 * Individual file change item for IntelliJ IDEA using Jewel components.
 *
 * Displays file name, path, change type icon, diff stats, and action buttons.
 */
@Composable
fun IdeaFileChangeItem(
    change: FileChange,
    onClick: () -> Unit,
    onUndo: () -> Unit,
    onKeep: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(JewelTheme.globalColors.panelBackground.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // File info
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Change type icon
            val iconKey = when (change.changeType) {
                ChangeType.CREATE -> AllIconsKeys.General.Add
                ChangeType.EDIT -> AllIconsKeys.Actions.Edit
                ChangeType.DELETE -> AllIconsKeys.General.Remove
                ChangeType.RENAME -> AllIconsKeys.Actions.Edit // Use Edit as fallback for Rename
            }
            val iconColor = when (change.changeType) {
                ChangeType.CREATE -> AutoDevColors.Green.c400
                ChangeType.EDIT -> AutoDevColors.Blue.c400
                ChangeType.DELETE -> AutoDevColors.Red.c400
                ChangeType.RENAME -> AutoDevColors.Indigo.c400 // Use Indigo instead of Purple
            }
            
            Icon(
                key = iconKey,
                contentDescription = change.changeType.name,
                modifier = Modifier.size(14.dp),
                tint = iconColor
            )

            // File name
            Text(
                text = change.getFileName(),
                style = JewelTheme.defaultTextStyle.copy(fontSize = 11.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            // Path separator
            Text(
                text = "\u00B7", // Middle dot
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 11.sp,
                    color = AutoDevColors.Neutral.c500
                )
            )

            // Parent path
            val parentPath = change.filePath.substringBeforeLast('/')
            if (parentPath.isNotEmpty()) {
                Text(
                    text = parentPath.substringAfterLast('/'),
                    style = JewelTheme.defaultTextStyle.copy(
                        fontSize = 10.sp,
                        color = AutoDevColors.Neutral.c500
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            // Diff stats
            val diffStats = change.getDiffStats()
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (diffStats.addedLines > 0) {
                    Text(
                        text = "+${diffStats.addedLines}",
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 10.sp,
                            color = AutoDevColors.Green.c400
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(AutoDevColors.Green.c900.copy(alpha = 0.3f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                if (diffStats.deletedLines > 0) {
                    Text(
                        text = "-${diffStats.deletedLines}",
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 10.sp,
                            color = AutoDevColors.Red.c400
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(AutoDevColors.Red.c900.copy(alpha = 0.3f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }

        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Keep button
            IconButton(
                onClick = onKeep,
                modifier = Modifier.size(22.dp)
            ) {
                Icon(
                    key = AllIconsKeys.Actions.Checked,
                    contentDescription = "Keep",
                    modifier = Modifier.size(12.dp),
                    tint = AutoDevColors.Green.c400
                )
            }

            // Undo button
            IconButton(
                onClick = onUndo,
                modifier = Modifier.size(22.dp)
            ) {
                Icon(
                    key = AllIconsKeys.Actions.Rollback,
                    contentDescription = "Undo",
                    modifier = Modifier.size(12.dp),
                    tint = AutoDevColors.Red.c400
                )
            }
        }
    }
}
