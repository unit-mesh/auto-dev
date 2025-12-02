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
import androidx.compose.ui.text.style.TextOverflow
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
 * Layout: Add Button | Selected Files... | Context indicator
 *
 * Features:
 * - Integrates with IdeaContextManager for state management
 * - Shows selected files as chips with remove button on hover
 * - Shows context indicator when default context or rules are active
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

    // Get context manager state if project is available
    val contextManager = remember(project) { project?.let { IdeaContextManager.getInstance(it) } }
    val hasDefaultContext by contextManager?.defaultContextFiles?.collectAsState()
        ?: remember { mutableStateOf(emptyList<VirtualFile>()) }
    val rules by contextManager?.rules?.collectAsState()
        ?: remember { mutableStateOf(emptyList<ContextRule>()) }
    val relatedFiles by contextManager?.relatedFiles?.collectAsState()
        ?: remember { mutableStateOf(emptyList<VirtualFile>()) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Add button with popup
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File search popup with trigger button
            if (project != null) {
                IdeaFileSearchPopup(
                    project = project,
                    showPopup = showFileSearchPopup,
                    onShowPopupChange = { showFileSearchPopup = it },
                    onFilesSelected = { files ->
                        onFilesSelected(files)
                        showFileSearchPopup = false
                    }
                )
            } else {
                ToolbarIconButton(
                    onClick = { onAddFileClick() },
                    tooltip = "Add File to Context"
                ) {
                    Icon(
                        imageVector = IdeaComposeIcons.Add,
                        contentDescription = "Add File",
                        modifier = Modifier.size(16.dp),
                        tint = JewelTheme.globalColors.text.normal
                    )
                }
            }

            // Context indicator: show if default context or rules are active
            if (hasDefaultContext.isNotEmpty() || rules.isNotEmpty()) {
                ContextIndicator(
                    hasDefaultContext = hasDefaultContext.isNotEmpty(),
                    rulesCount = rules.size
                )
            }
        }

        if (selectedFiles.isNotEmpty() || relatedFiles.isNotEmpty()) {
            Box(Modifier.width(1.dp).height(20.dp).background(JewelTheme.globalColors.borders.normal))
        }

        // Selected files as chips
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Show selected files
            selectedFiles.take(5).forEach { file ->
                FileChip(file = file, onRemove = { onRemoveFile(file) })
            }

            // Show overflow indicator if more than 5 files
            if (selectedFiles.size > 5) {
                Text(
                    text = "+${selectedFiles.size - 5}",
                    style = JewelTheme.defaultTextStyle.copy(
                        fontSize = 11.sp,
                        color = JewelTheme.globalColors.text.normal.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
}

/**
 * Context indicator showing active default context or rules
 */
@Composable
private fun ContextIndicator(
    hasDefaultContext: Boolean,
    rulesCount: Int
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val tooltipText = buildString {
        if (hasDefaultContext) append("Default context active")
        if (hasDefaultContext && rulesCount > 0) append(" | ")
        if (rulesCount > 0) append("$rulesCount rule(s) active")
    }

    Tooltip(tooltip = { Text(tooltipText) }) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .hoverable(interactionSource = interactionSource)
                .background(
                    if (isHovered) JewelTheme.globalColors.borders.normal.copy(alpha = 0.3f)
                    else JewelTheme.globalColors.panelBackground.copy(alpha = 0.5f)
                )
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasDefaultContext) {
                Icon(
                    imageVector = IdeaComposeIcons.Book,
                    contentDescription = "Default context",
                    modifier = Modifier.size(12.dp),
                    tint = JewelTheme.globalColors.text.info
                )
            }
            if (rulesCount > 0) {
                Icon(
                    imageVector = IdeaComposeIcons.Settings,
                    contentDescription = "Rules",
                    modifier = Modifier.size(12.dp),
                    tint = JewelTheme.globalColors.text.info
                )
                Text(
                    text = rulesCount.toString(),
                    style = JewelTheme.defaultTextStyle.copy(
                        fontSize = 10.sp,
                        color = JewelTheme.globalColors.text.info
                    )
                )
            }
        }
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

