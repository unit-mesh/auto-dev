package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.agent.ReviewTask
import cc.unitmesh.agent.ReviewType

/**
 * Input component specifically for Code Review Agent
 * Allows users to configure review parameters and start reviews
 */
@Composable
fun CodeReviewInput(
    projectPath: String,
    onReview: (ReviewTask) -> Unit,
    isExecuting: Boolean = false,
    modifier: Modifier = Modifier
) {
    var selectedReviewType by remember { mutableStateOf(ReviewType.COMPREHENSIVE) }
    var additionalContext by remember { mutableStateOf("") }
    var filePathsInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Text(
            text = "Code Review Configuration",
            style = MaterialTheme.typography.titleMedium
        )

        // Review Type Selection
        Text(
            text = "Review Type",
            style = MaterialTheme.typography.labelMedium
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ReviewType.entries.forEach { type ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedReviewType == type,
                        onClick = { selectedReviewType = type }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = type.name.replace("_", " ").lowercase()
                                .replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = getReviewTypeDescription(type),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // File Paths Input (optional)
        OutlinedTextField(
            value = filePathsInput,
            onValueChange = { filePathsInput = it },
            label = { Text("File Paths (optional)") },
            placeholder = { Text("e.g., src/main.kt, src/utils/helper.kt") },
            supportingText = {
                Text("Leave empty to review the entire project. Separate multiple files with commas.")
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )

        // Additional Context Input
        OutlinedTextField(
            value = additionalContext,
            onValueChange = { additionalContext = it },
            label = { Text("Additional Context (optional)") },
            placeholder = { Text("Any specific concerns or focus areas...") },
            supportingText = {
                Text("Provide any additional context to help the reviewer understand your needs.")
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 6
        )

        // Start Review Button
        Button(
            onClick = {
                println("🎬 [CodeReviewInput] Start Review button clicked")
                val filePaths = if (filePathsInput.isBlank()) {
                    emptyList()
                } else {
                    filePathsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                }

                val task = ReviewTask(
                    filePaths = filePaths,
                    reviewType = selectedReviewType,
                    projectPath = projectPath,
                    additionalContext = additionalContext
                )

                println("📤 [CodeReviewInput] Calling onReview with task: ${task.reviewType}")
                onReview(task)
            },
            enabled = !isExecuting,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isExecuting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isExecuting) "Reviewing..." else "Start Review")
        }

        // Info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "💡 Tip",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "The Code Review Agent will analyze your code for quality, security, performance, and best practices. " +
                            "Review results will appear in the chat timeline below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getReviewTypeDescription(type: ReviewType): String {
    return when (type) {
        ReviewType.COMPREHENSIVE -> "Complete analysis: quality, security, performance, style"
        ReviewType.SECURITY -> "Focus on security vulnerabilities and best practices"
        ReviewType.PERFORMANCE -> "Focus on performance optimization opportunities"
        ReviewType.STYLE -> "Focus on code style and conventions"
    }
}
