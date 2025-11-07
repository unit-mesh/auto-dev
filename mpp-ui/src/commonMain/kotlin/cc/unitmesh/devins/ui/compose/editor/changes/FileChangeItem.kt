package cc.unitmesh.devins.ui.compose.editor.changes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.agent.tool.tracking.ChangeType
import cc.unitmesh.agent.tool.tracking.FileChange

/**
 * Individual file change item
 */
@Composable
fun FileChangeItem(
    change: FileChange,
    onClick: () -> Unit,
    onUndo: () -> Unit,
    onKeep: () -> Unit,
    modifier: Modifier = Modifier.Companion
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick), // Make the entire item clickable
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            // File info
            Row(
                modifier = Modifier.Companion.weight(1f),
                verticalAlignment = Alignment.Companion.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Change type icon
                Icon(
                    imageVector = when (change.changeType) {
                        ChangeType.CREATE -> Icons.Default.Add
                        ChangeType.EDIT -> Icons.Default.Edit
                        ChangeType.DELETE -> Icons.Default.Delete
                        ChangeType.OVERWRITE -> Icons.Default.Create
                    },
                    contentDescription = change.changeType.name,
                    modifier = Modifier.Companion.size(16.dp),
                    tint = when (change.changeType) {
                        ChangeType.CREATE -> MaterialTheme.colorScheme.primary
                        ChangeType.EDIT -> MaterialTheme.colorScheme.tertiary
                        ChangeType.DELETE -> MaterialTheme.colorScheme.error
                        ChangeType.OVERWRITE -> MaterialTheme.colorScheme.secondary
                    }
                )

                // File name and path - single line compact version
                Row(
                    modifier = Modifier.Companion.weight(1f),
                    verticalAlignment = Alignment.Companion.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = change.getFileName(),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Companion.Monospace,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Companion.Ellipsis,
                        modifier = Modifier.Companion.weight(1f, fill = false)
                    )
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = change.filePath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Companion.Monospace,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Companion.Ellipsis,
                        modifier = Modifier.Companion.weight(1f, fill = true)
                    )
                }

                // Accurate diff stats indicator (using LCS algorithm)
                val diffStats = change.getDiffStats()
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (diffStats.addedLines > 0) {
                        Text(
                            text = "+${diffStats.addedLines}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Companion.Monospace,
                            fontSize = 10.sp,
                            modifier = Modifier.Companion
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    if (diffStats.deletedLines > 0) {
                        Text(
                            text = "-${diffStats.deletedLines}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontFamily = FontFamily.Companion.Monospace,
                            fontSize = 10.sp,
                            modifier = Modifier.Companion
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // Action buttons - more compact
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Keep button
                IconButton(
                    onClick = onKeep,
                    modifier = Modifier.Companion.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Keep",
                        modifier = Modifier.Companion.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Undo button
                IconButton(
                    onClick = onUndo,
                    modifier = Modifier.Companion.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Undo",
                        modifier = Modifier.Companion.size(14.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
