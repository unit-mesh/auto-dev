package cc.unitmesh.devins.ui.compose.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cc.unitmesh.agent.tool.tracking.ChangeType
import cc.unitmesh.agent.tool.tracking.FileChange
import cc.unitmesh.agent.tool.tracking.FileChangeTracker
import cc.unitmesh.agent.util.DiffUtils
import cc.unitmesh.devins.ui.compose.sketch.DiffSketchRenderer
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
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Collapsed header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
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
                
                // Action buttons
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
                        colors = ButtonDefaults.textButtonColors(
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
                        colors = ButtonDefaults.textButtonColors(
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
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

/**
 * Individual file change item
 */
@Composable
private fun FileChangeItem(
    change: FileChange,
    onClick: () -> Unit,
    onUndo: () -> Unit,
    onKeep: () -> Unit,
    modifier: Modifier = Modifier
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
            modifier = Modifier
                .fillMaxWidth()
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
                Icon(
                    imageVector = when (change.changeType) {
                        ChangeType.CREATE -> Icons.Default.Add
                        ChangeType.EDIT -> Icons.Default.Edit
                        ChangeType.DELETE -> Icons.Default.Delete
                        ChangeType.OVERWRITE -> Icons.Default.Create
                    },
                    contentDescription = change.changeType.name,
                    modifier = Modifier.size(16.dp),
                    tint = when (change.changeType) {
                        ChangeType.CREATE -> MaterialTheme.colorScheme.primary
                        ChangeType.EDIT -> MaterialTheme.colorScheme.tertiary
                        ChangeType.DELETE -> MaterialTheme.colorScheme.error
                        ChangeType.OVERWRITE -> MaterialTheme.colorScheme.secondary
                    }
                )
                
                // File name and path - single line compact version
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = change.getFileName(),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
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
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = true)
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
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    if (diffStats.deletedLines > 0) {
                        Text(
                            text = "-${diffStats.deletedLines}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
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
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Keep",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Undo button
                IconButton(
                    onClick = onUndo,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Undo",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Dialog to show diff view for a file change
 */
@Composable
private fun DiffViewDialog(
    change: FileChange,
    onDismiss: () -> Unit,
    onUndo: () -> Unit,
    onKeep: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header with file info and actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = change.getFileName(),
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = change.filePath,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            onUndo()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Undo",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Undo")
                        }
                        
                        Button(onClick = {
                            onKeep()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Keep",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Keep")
                        }
                        
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    }
                }
                
                HorizontalDivider()
                
                // Diff content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Generate unified diff format
                    val diffContent = remember(change) {
                        DiffUtils.generateUnifiedDiff(
                            change.originalContent,
                            change.newContent,
                            change.filePath
                        )
                    }
                    
                    if (diffContent.isNotBlank()) {
                        DiffSketchRenderer.RenderDiff(
                            diffContent = diffContent,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Fallback for new files or empty diffs
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = when (change.changeType) {
                                    ChangeType.CREATE -> Icons.Default.Add
                                    ChangeType.DELETE -> Icons.Default.Delete
                                    else -> Icons.Default.Edit
                                },
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = when (change.changeType) {
                                    ChangeType.CREATE -> "New file created"
                                    ChangeType.DELETE -> "File deleted"
                                    else -> "File modified"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val stats = change.getDiffStats()
                            Text(
                                text = "+${stats.addedLines} -${stats.deletedLines} lines",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

