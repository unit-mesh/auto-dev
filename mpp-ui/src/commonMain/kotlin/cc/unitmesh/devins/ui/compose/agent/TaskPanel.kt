package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.unitmesh.agent.Platform
import cc.unitmesh.agent.render.TaskInfo
import cc.unitmesh.agent.render.TaskStatus
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors

/**
 * UI extension for TaskStatus - provides icon and color for display
 */
@Composable
fun TaskStatus.icon(): Unit = when (this) {
    TaskStatus.PLANNING -> Icon(Icons.Default.Create, null)
    TaskStatus.WORKING -> Icon(Icons.Default.Build, null)
    TaskStatus.COMPLETED -> Icon(Icons.Default.CheckCircle, null)
    TaskStatus.BLOCKED -> Icon(Icons.Default.Warning, null)
    TaskStatus.CANCELLED -> Icon(Icons.Default.Cancel, null)
}

val TaskStatus.color: Color
    get() = when (this) {
        TaskStatus.PLANNING -> Color(0xFF9C27B0)
        TaskStatus.WORKING -> Color(0xFF2196F3)
        TaskStatus.COMPLETED -> Color(0xFF4CAF50)
        TaskStatus.BLOCKED -> Color(0xFFFF9800)
        TaskStatus.CANCELLED -> Color(0xFF9E9E9E)
    }

/**
 * Task Panel Component - displays active tasks from task-boundary tool
 */
@Composable
fun TaskPanel(
    tasks: List<TaskInfo>,
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {}
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = "Tasks",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Tasks",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (tasks.isNotEmpty()) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(tasks.size.toString())
                            }
                        }
                    }

                    IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Divider()

            // Task List
            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            "No active tasks",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tasks, key = { "${it.taskName}_${it.timestamp}" }) { task ->
                        TaskCard(task = task)
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskCard(task: TaskInfo, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = task.status.color.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Task Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = task.status.color.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (task.status == TaskStatus.WORKING) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .rotate(angle),
                                    tint = task.status.color
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(task.status.color, CircleShape)
                                )
                            }
                        }
                        Text(
                            task.status.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = task.status.color,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Time elapsed
                val elapsed = (Platform.getCurrentTimestamp() - task.startTime) / 1000
                Text(
                    formatDuration(elapsed),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Task Name
            Text(
                task.taskName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Summary
            if (task.summary.isNotEmpty()) {
                Text(
                    task.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodySmall.fontSize.times(1.4)
                )
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
        else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    }
}

