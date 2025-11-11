package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.ui.compose.terminal.ProcessTtyConnector
import cc.unitmesh.devins.ui.compose.terminal.TerminalWidget
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors

/**
 * JVM implementation of LiveTerminalItem.
 * Uses JediTerm with PTY process for real-time terminal emulation.
 *
 * Modern compact design inspired by IntelliJ Terminal plugin:
 * - Compact header (32-36dp) to save space in timeline
 * - Inline status indicator
 * - Clean, minimal design using AutoDevColors
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

    val isRunning = process?.isAlive == true

    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
        shape = RoundedCornerShape(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        // Smooth height changes
        Column(
            modifier =
                Modifier
                    .padding(8.dp)
                    .animateContentSize()
        ) {
            // Compact header - inspired by IntelliJ Terminal
            // Compact height: 32dp
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Collapse/Expand icon
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )

                // Status indicator - small dot
                Box(
                    modifier =
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isRunning) AutoDevColors.Green.c400 else MaterialTheme.colorScheme.outline
                            )
                )

                // Terminal icon + command in one line
                Text(
                    text = "💻",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 14.sp
                )

                // Command text - truncated if too long
                Text(
                    text = command,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

//                 Status badge - compact
                Surface(
                    color =
                        if (isRunning) {
                            AutoDevColors.Green.c400.copy(alpha = 0.15f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(20.dp)
                ) {
                    Text(
                        text = if (isRunning) "RUNNING" else "DONE",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color =
                            if (isRunning) {
                                AutoDevColors.Green.c400
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Working directory - only show when expanded and exists
            if (expanded && workingDirectory != null) {
                Text(
                    text = "📁 $workingDirectory",
                    modifier = Modifier.padding(start = 30.dp, top = 2.dp, bottom = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(4.dp))

                if (ttyConnector != null) {
                    val terminalHeight = 60.dp

                    TerminalWidget(
                        ttyConnector = ttyConnector,
                        modifier = Modifier.fillMaxWidth().height(terminalHeight)
                    )
                } else {
                    Card(
                        colors = CardDefaults.cardColors(
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
