package cc.unitmesh.devins.ui.compose.editor.changes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cc.unitmesh.agent.diff.ChangeType
import cc.unitmesh.agent.diff.FileChange
import cc.unitmesh.agent.diff.DiffUtils
import cc.unitmesh.devins.ui.compose.sketch.DiffSketchRenderer

/**
 * Dialog to show diff view for a file change
 */
@Composable
fun DiffViewDialog(
    change: FileChange,
    onDismiss: () -> Unit,
    onUndo: () -> Unit,
    onKeep: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier =
                        Modifier
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
                            fontFamily = FontFamily.Companion.Monospace
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
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                ) {
                    val diffContent =
                        remember(change) {
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
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.Companion.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector =
                                    when (change.changeType) {
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
                                text =
                                    when (change.changeType) {
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
