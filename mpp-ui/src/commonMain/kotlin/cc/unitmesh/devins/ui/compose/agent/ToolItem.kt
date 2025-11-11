package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons

@Composable
fun ToolResultItem(
    toolName: String,
    success: Boolean,
    summary: String,
    output: String?,
    fullOutput: String? = null
) {
    var expanded by remember { mutableStateOf(!success) }
    var showFullOutput by remember { mutableStateOf(!success) }
    val clipboardManager = LocalClipboardManager.current

    val displayOutput = if (showFullOutput) fullOutput else output
    val hasFullOutput = fullOutput != null && fullOutput != output

    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
        shape = RoundedCornerShape(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.Companion.padding(8.dp)) {
            Row(
                modifier =
                    Modifier.Companion
                        .fillMaxWidth()
                        .clickable { if (displayOutput != null) expanded = !expanded },
                verticalAlignment = Alignment.Companion.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (success) "✓" else "✗",
                    color =
                        if (success) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                )
                Text(
                    text = toolName,
                    fontWeight = FontWeight.Companion.Medium,
                    color =
                        if (success) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                    modifier = Modifier.Companion.weight(1f)
                )
                Text(
                    text = "→ $summary",
                    color =
                        if (success) {
                            Color(0xFF4CAF50)
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Companion.Medium
                )

                if (displayOutput != null) {
                    Icon(
                        imageVector = if (expanded) AutoDevComposeIcons.ExpandLess else AutoDevComposeIcons.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint =
                            if (success) {
                                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                            },
                        modifier = Modifier.Companion.size(20.dp)
                    )
                }
            }

            if (expanded && displayOutput != null) {
                Spacer(modifier = Modifier.Companion.height(8.dp))

                Row(
                    modifier = Modifier.Companion.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Companion.Top
                ) {
                    Column {
                        Text(
                            text = "Output:",
                            color =
                                if (success) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                },
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (hasFullOutput) {
                            TextButton(
                                onClick = { showFullOutput = !showFullOutput },
                                modifier = Modifier.Companion.height(32.dp)
                            ) {
                                Text(
                                    text = if (showFullOutput) "Show Less" else "Show Full Output",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Row {
                        IconButton(
                            onClick = { clipboardManager.setText(AnnotatedString(displayOutput ?: "")) },
                            modifier = Modifier.Companion.size(24.dp)
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.ContentCopy,
                                contentDescription = "Copy output",
                                tint =
                                    if (success) {
                                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    } else {
                                        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                                    },
                                modifier = Modifier.Companion.size(16.dp)
                            )
                        }

                        // Copy entire block button (always copy full output if available)
                        IconButton(
                            onClick = {
                                val blockText =
                                    buildString {
                                        val status = if (success) "SUCCESS" else "FAILED"
                                        appendLine("[Tool Result]: $toolName - $status")
                                        appendLine("Summary: $summary")
                                        appendLine("Output: ${fullOutput ?: output ?: ""}")
                                    }
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(blockText))
                            },
                            modifier = Modifier.Companion.size(24.dp)
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.ContentCopy,
                                contentDescription = "Copy entire block",
                                tint =
                                    if (success) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    },
                                modifier = Modifier.Companion.size(16.dp)
                            )
                        }
                    }
                }

                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                    modifier = Modifier.Companion.fillMaxWidth()
                ) {
                    Text(
                        text = formatOutput(displayOutput),
                        modifier = Modifier.Companion.padding(8.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Companion.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun ToolErrorItem(
    error: String,
    onDismiss: () -> Unit
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "❌ Error",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
            }

            Text(
                text = error,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
