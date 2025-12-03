package cc.unitmesh.devins.ui.compose.editor.context

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons

/**
 * FileChip Component
 *
 * Displays a selected file as a removable chip.
 * Similar to IdeaTopToolbar's FileChip - shows remove button only on hover.
 */
@Composable
fun FileChip(
    file: SelectedFileItem,
    onRemove: () -> Unit,
    showPath: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .hoverable(interactionSource = interactionSource)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(4.dp)
            ),
        shape = RoundedCornerShape(4.dp),
        color = if (isHovered) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        },
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // File/Folder icon
            Icon(
                imageVector = if (file.isDirectory) AutoDevComposeIcons.Folder else AutoDevComposeIcons.InsertDriveFile,
                contentDescription = if (file.isDirectory) "Folder" else "File",
                modifier = Modifier.size(14.dp),
                tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else LocalContentColor.current
            )

            // File name
            Text(
                text = file.name,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Optional path (truncated)
            if (showPath && file.truncatedPath.isNotEmpty()) {
                Text(
                    text = file.truncatedPath,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = LocalContentColor.current.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 100.dp)
                )
            }

            // Remove button - only show on hover (like IDEA version)
            if (isHovered) {
                Icon(
                    imageVector = AutoDevComposeIcons.Close,
                    contentDescription = "Remove from context",
                    modifier = Modifier
                        .size(14.dp)
                        .clickable(onClick = onRemove),
                    tint = LocalContentColor.current.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * Expanded FileChip for vertical list view.
 * Similar to IdeaTopToolbar's FileChipExpanded - shows full path and hover effect.
 */
@Composable
fun FileChipExpanded(
    file: SelectedFileItem,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .hoverable(interactionSource = interactionSource)
            .background(
                if (isHovered) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // File/Folder icon
        Icon(
            imageVector = if (file.isDirectory) AutoDevComposeIcons.Folder else AutoDevComposeIcons.InsertDriveFile,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )

        // File info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = file.path,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Remove button - always visible in expanded mode but with hover effect
        Icon(
            imageVector = AutoDevComposeIcons.Close,
            contentDescription = "Remove from context",
            modifier = Modifier
                .size(16.dp)
                .clickable(onClick = onRemove),
            tint = if (isHovered) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            }
        )
    }
}

