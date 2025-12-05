package cc.unitmesh.devins.idea.toolwindow.changes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.agent.diff.ChangeType
import cc.unitmesh.agent.diff.FileChange
import cc.unitmesh.agent.diff.FileChangeTracker
import cc.unitmesh.devins.idea.compose.IdeaLaunchedEffect
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys

private val logger = Logger.getInstance("IdeaFileChangeSummary")

/**
 * File Change Summary Component for IntelliJ IDEA using Jewel components.
 *
 * Displays a collapsible summary of all file changes made by the AI Agent.
 * Uses Jewel theming and components for native IntelliJ look and feel.
 */
@Composable
fun IdeaFileChangeSummary(
    project: Project,
    modifier: Modifier = Modifier
) {
    // Use manual state collection to avoid ClassLoader conflicts with collectAsState()
    var changes by remember { mutableStateOf(emptyList<FileChange>()) }
    IdeaLaunchedEffect(Unit) {
        FileChangeTracker.changes.collect { changes = it }
    }
    var isExpanded by remember { mutableStateOf(false) }
    var selectedChange by remember { mutableStateOf<FileChange?>(null) }

    // Only show if there are changes
    if (changes.isEmpty()) {
        return
    }

    // Handle diff dialog display when a file is selected
    IdeaLaunchedEffect(selectedChange) {
        selectedChange?.let { change ->
            ApplicationManager.getApplication().invokeLater {
                IdeaFileChangeDiffDialogWrapper.show(
                    project = project,
                    change = change,
                    onUndo = {
                        undoChange(project, change)
                        FileChangeTracker.removeChange(change)
                    },
                    onKeep = {
                        FileChangeTracker.removeChange(change)
                    },
                    onDismiss = {}
                )
            }
            selectedChange = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                JewelTheme.globalColors.panelBackground,
                RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
            )
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
    ) {
        // Collapsed header
        IdeaChangeSummaryHeader(
            changeCount = changes.size,
            isExpanded = isExpanded,
            onExpandToggle = { isExpanded = !isExpanded },
            onUndoAll = {
                changes.forEach { change ->
                    undoChange(project, change)
                }
                FileChangeTracker.clearChanges()
            },
            onKeepAll = {
                FileChangeTracker.clearChanges()
            }
        )

        // Expanded content
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(JewelTheme.globalColors.borders.normal)
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(changes, key = { "${it.filePath}_${it.timestamp}" }) { change ->
                        IdeaFileChangeItem(
                            change = change,
                            onClick = { selectedChange = change },
                            onUndo = {
                                undoChange(project, change)
                                FileChangeTracker.removeChange(change)
                            },
                            onKeep = {
                                FileChangeTracker.removeChange(change)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IdeaChangeSummaryHeader(
    changeCount: Int,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onUndoAll: () -> Unit,
    onKeepAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandToggle() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: expand arrow and title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Expand arrow
            Icon(
                key = if (isExpanded) AllIconsKeys.General.ArrowDown else AllIconsKeys.General.ArrowRight,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                modifier = Modifier.size(12.dp),
                tint = AutoDevColors.Neutral.c400
            )

            // Title
            Text(
                text = "$changeCount file${if (changeCount > 1) "s" else ""} changed",
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Right side: action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Undo All button
            IconButton(
                onClick = onUndoAll,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    key = AllIconsKeys.Actions.Rollback,
                    contentDescription = "Undo All",
                    modifier = Modifier.size(14.dp),
                    tint = AutoDevColors.Red.c400
                )
            }

            // Keep All button
            IconButton(
                onClick = onKeepAll,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    key = AllIconsKeys.Actions.Checked,
                    contentDescription = "Keep All",
                    modifier = Modifier.size(14.dp),
                    tint = AutoDevColors.Green.c400
                )
            }
        }
    }
}

/**
 * Undo a file change using IntelliJ's RollbackWorker for proper VCS integration.
 * Falls back to manual revert if RollbackWorker fails.
 */
private fun undoChange(project: Project, fileChange: FileChange) {
    ApplicationManager.getApplication().invokeLater {
        try {
            // Convert FileChange to IntelliJ Change and use RollbackWorker
            val change = FileChangeConverter.toChange(project, fileChange)
            val rollbackWorker = com.intellij.openapi.vcs.changes.ui.RollbackWorker(project)
            rollbackWorker.doRollback(listOf(change), false)
        } catch (e: Exception) {
            logger.warn("RollbackWorker failed, falling back to manual revert", e)
            // Fallback to manual revert
            performManualUndo(project, fileChange)
        }
    }
}

/**
 * Manual undo fallback when RollbackWorker fails.
 */
private fun performManualUndo(project: Project, change: FileChange) {
    runWriteAction {
        try {
            when (change.changeType) {
                ChangeType.CREATE -> {
                    // For created files, delete or clear the content
                    val virtualFile = LocalFileSystem.getInstance().findFileByPath(change.filePath)
                    virtualFile?.let { vf ->
                        val document = FileDocumentManager.getInstance().getDocument(vf)
                        document?.setText("")
                    }
                }
                ChangeType.EDIT, ChangeType.RENAME -> {
                    // Restore original content
                    change.originalContent?.let { original ->
                        val virtualFile = LocalFileSystem.getInstance().findFileByPath(change.filePath)
                        virtualFile?.let { vf ->
                            val document = FileDocumentManager.getInstance().getDocument(vf)
                            document?.setText(original)
                        }
                    }
                }
                ChangeType.DELETE -> {
                    // For deleted files, we would need to recreate them
                    change.originalContent?.let { original ->
                        val parentPath = change.filePath.substringBeforeLast('/')
                        val fileName = change.filePath.substringAfterLast('/')
                        val parentDir = LocalFileSystem.getInstance().findFileByPath(parentPath)
                        parentDir?.let { dir ->
                            val newFile = dir.createChildData(project, fileName)
                            val document = FileDocumentManager.getInstance().getDocument(newFile)
                            document?.setText(original)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to undo change for ${change.filePath}", e)
        }
    }
}
