package cc.unitmesh.devins.idea.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip

/**
 * Top toolbar for the input section.
 * Contains @ trigger, file selection, and other context-related actions.
 *
 * Layout: @ - / - Clipboard - Save - Cursor | Selected Files... | Add
 */
@Composable
fun IdeaTopToolbar(
    project: Project? = null,
    onAtClick: () -> Unit = {},
    onSlashClick: () -> Unit = {},
    onAddFileClick: () -> Unit = {},
    selectedFiles: List<SelectedFileItem> = emptyList(),
    onRemoveFile: (SelectedFileItem) -> Unit = {},
    onFilesSelected: (List<VirtualFile>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showFileSearchPopup by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarIconButton(
                onClick = {
                    if (project != null) {
                        showFileSearchPopup = true
                    }
                    onAddFileClick()
                },
                tooltip = "Add File"
            ) {
                Icon(
                    imageVector = IdeaComposeIcons.Add,
                    contentDescription = "Add File",
                    modifier = Modifier.size(16.dp),
                    tint = JewelTheme.globalColors.text.normal
                )
            }
        }

        if (selectedFiles.isNotEmpty()) {
            Box(Modifier.width(1.dp).height(20.dp).background(JewelTheme.globalColors.borders.normal))
        }

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            selectedFiles.forEach { file ->
                FileChip(file = file, onRemove = { onRemoveFile(file) })
            }
        }
    }

    // File search popup
    if (showFileSearchPopup && project != null) {
        IdeaFileSearchPopup(
            project = project,
            onDismiss = { showFileSearchPopup = false },
            onFilesSelected = { files ->
                onFilesSelected(files)
                showFileSearchPopup = false
            }
        )
    }
}

@Composable
private fun ToolbarIconButton(
    onClick: () -> Unit,
    tooltip: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Tooltip(tooltip = { Text(tooltip) }) {
        Box(
            modifier = modifier
                .size(28.dp)
                .clip(RoundedCornerShape(4.dp))
                .hoverable(interactionSource = interactionSource)
                .background(
                    if (isHovered) JewelTheme.globalColors.borders.normal.copy(alpha = 0.3f)
                    else androidx.compose.ui.graphics.Color.Transparent
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) { content() }
    }
}

@Composable
private fun FileChip(file: SelectedFileItem, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .hoverable(interactionSource = interactionSource)
            .background(JewelTheme.globalColors.panelBackground.copy(alpha = 0.8f))
            .border(1.dp, JewelTheme.globalColors.borders.normal, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = file.icon ?: IdeaComposeIcons.InsertDriveFile,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = JewelTheme.globalColors.text.normal
        )
        Text(text = file.name, style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp), maxLines = 1)
        if (isHovered) {
            Icon(
                imageVector = IdeaComposeIcons.Close,
                contentDescription = "Remove",
                modifier = Modifier.size(14.dp).clickable(onClick = onRemove),
                tint = JewelTheme.globalColors.text.normal.copy(alpha = 0.6f)
            )
        }
    }
}

data class SelectedFileItem(
    val name: String,
    val path: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    val virtualFile: com.intellij.openapi.vfs.VirtualFile? = null
)

