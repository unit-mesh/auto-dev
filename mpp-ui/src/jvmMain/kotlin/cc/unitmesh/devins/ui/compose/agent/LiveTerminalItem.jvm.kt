package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.compose.terminal.ProcessTtyConnector
import cc.unitmesh.devins.ui.compose.terminal.TerminalWidget

/**
 * JVM implementation of LiveTerminalItem.
 * Uses JediTerm with PTY process for real-time terminal emulation.
 */
@Composable
actual fun LiveTerminalItem(
    sessionId: String,
    command: String,
    workingDirectory: String?,
    ptyHandle: Any?
) {
    var expanded by remember { mutableStateOf(true) } // Auto-expand live terminal
    val process =
        remember(ptyHandle) {
            if (ptyHandle is Process) {
                ptyHandle
            } else {
                null
            }
        }

    // Create TtyConnector from the process
    val ttyConnector =
        remember(process) {
            process?.let { ProcessTtyConnector(it) }
        }

    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
        shape = RoundedCornerShape(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier =
                Modifier
                    .padding(8.dp)
                    .animateContentSize() // Smooth height changes
        ) {
            // Header row with collapse button
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Collapse/Expand icon
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "💻",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Live Terminal",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                // Status indicator
                if (process?.isAlive == true) {
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "RUNNING",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "COMPLETED",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$ $command",
                modifier = Modifier.padding(start = 28.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )

            if (workingDirectory != null) {
                Text(
                    text = "Working directory: $workingDirectory",
                    modifier = Modifier.padding(start = 28.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))

                if (ttyConnector != null) {
                    // Dynamic height based on content, similar to IDEA's terminal implementation
                    // Minimum: ~4 lines (100dp), Maximum: ~20 lines (400dp)
                    // This provides a better UX than full-height terminal
                    val terminalHeight = 300.dp // Default to ~15 lines, good balance for most commands

                    TerminalWidget(
                        ttyConnector = ttyConnector,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(terminalHeight)
                    )
                } else {
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚠️ Failed to connect to terminal process",
                            modifier = Modifier.padding(8.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
