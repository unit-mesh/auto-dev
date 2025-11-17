package cc.unitmesh.devins.ui.compose.editor.changes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.agent.diff.ChangeType
import cc.unitmesh.agent.diff.FileChange
import cc.unitmesh.agent.diff.FileChangeTracker
import cc.unitmesh.devins.workspace.WorkspaceManager
import kotlinx.coroutines.launch

/**
 * File Change Summary Component
 *
 * Displays a collapsible summary of all file changes made by the AI Agent.
 * Similar to AutoDev IDEA's PlannerResultSummary.
 */
@Composable
fun FileChangeSummary(
    modifier: Modifier = Modifier
) {
    val changes by FileChangeTracker.changes.collectAsState()
    var isExpanded by remember { mutableStateOf(false) }
    var selectedChange by remember { mutableStateOf<FileChange?>(null) }
    val scope = rememberCoroutineScope()

    // Get workspace file system for undo operations
    val workspace = WorkspaceManager.currentWorkspace
    val fileSystem = workspace?.fileSystem

    // Only show if there are changes
    if (changes.isEmpty()) {
        return
    }

    // Show diff dialog if a file is selected
    selectedChange?.let { change ->
        DiffViewDialog(
            change = change,
            onDismiss = { selectedChange = null },
            onUndo = {
                scope.launch {
                    fileSystem?.let { fs ->
                        try {
                            val original = change.originalContent
                            when {
                                change.changeType == ChangeType.CREATE -> {
                                    if (fs.exists(change.filePath)) {
                                        fs.writeFile(change.filePath, "")
                                    }
                                }
                                original != null -> {
                                    fs.writeFile(change.filePath, original)
                                }
                            }
                            FileChangeTracker.removeChange(change)
                            selectedChange = null
                        } catch (e: Exception) {
                            println("Failed to undo change: ${e.message}")
                        }
                    }
                }
            },
            onKeep = {
                FileChangeTracker.removeChange(change)
                selectedChange = null
            }
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(topEnd = 4.dp, topStart = 4.dp, bottomEnd = 0.dp, bottomStart = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Collapsed header
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
                        .padding(horizontal = 4.dp, vertical = 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${changes.size} file${if (changes.size > 1) "s" else ""} changed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Undo All button
                    TextButton(
                        onClick = {
                            scope.launch {
                                // Restore all files to their original content
                                fileSystem?.let { fs ->
                                    changes.forEach { change ->
                                        try {
                                            val original = change.originalContent
                                            when {
                                                change.changeType == ChangeType.CREATE -> {
                                                    // Clear the created file by writing empty content
                                                    if (fs.exists(change.filePath)) {
                                                        fs.writeFile(change.filePath, "")
                                                    }
                                                }
                                                original != null -> {
                                                    // Restore original content
                                                    fs.writeFile(change.filePath, original)
                                                }
                                            }
                                        } catch (e: Exception) {
                                            println("Failed to undo change for ${change.filePath}: ${e.message}")
                                        }
                                    }
                                }
                                // Clear all changes after undo
                                FileChangeTracker.clearChanges()
                            }
                        },
                        colors =
                            ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Undo All",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Undo All", fontSize = 12.sp)
                    }

                    // Keep All button
                    TextButton(
                        onClick = {
                            // Just clear the change tracking (keep the files as they are)
                            FileChangeTracker.clearChanges()
                        },
                        colors =
                            ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Keep All",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Keep All", fontSize = 12.sp)
                    }
                }
            }

            // Expanded content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(changes, key = { it.timestamp }) { change ->
                            FileChangeItem(
                                change = change,
                                onClick = {
                                    // Show diff view when clicked
                                    selectedChange = change
                                },
                                onUndo = {
                                    scope.launch {
                                        fileSystem?.let { fs ->
                                            try {
                                                val original = change.originalContent
                                                when {
                                                    change.changeType == ChangeType.CREATE -> {
                                                        // Clear the created file by writing empty content
                                                        if (fs.exists(change.filePath)) {
                                                            fs.writeFile(change.filePath, "")
                                                        }
                                                    }
                                                    original != null -> {
                                                        // Restore original content
                                                        fs.writeFile(change.filePath, original)
                                                    }
                                                }
                                                FileChangeTracker.removeChange(change)
                                            } catch (e: Exception) {
                                                println("Failed to undo change: ${e.message}")
                                            }
                                        }
                                    }
                                },
                                onKeep = {
                                    // Remove from tracking only
                                    FileChangeTracker.removeChange(change)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
