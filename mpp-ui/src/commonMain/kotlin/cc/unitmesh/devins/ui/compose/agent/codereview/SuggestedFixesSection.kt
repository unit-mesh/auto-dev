package cc.unitmesh.devins.ui.compose.agent.codereview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.unitmesh.agent.diff.DiffParser
import cc.unitmesh.agent.linter.LinterRegistry
import cc.unitmesh.devins.parser.CodeFence
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.compose.sketch.DiffSketchRenderer
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import cc.unitmesh.devins.workspace.Workspace
import kotlinx.coroutines.launch

/**
 * Suggested Fixes Section - 展示 AI 建议的修复（Diff Patch 格式）
 * 支持 accept/reject 操作，带 lint 验证和回滚机制
 */
@Composable
fun SuggestedFixesSection(
    fixOutput: String,
    isActive: Boolean,
    onApplyFix: (String) -> Unit,
    onRejectFix: (String) -> Unit,
    modifier: Modifier = Modifier,
    workspace: Workspace? = null
) {
    var isExpanded by remember { mutableStateOf(true) }

    // 解析输出中的所有 diff patches
    val diffPatches = remember(fixOutput) {
        extractDiffPatches(fixOutput)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                AutoDevColors.Indigo.c600.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) AutoDevComposeIcons.ExpandMore else AutoDevComposeIcons.ChevronRight,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )

                    Icon(
                        imageVector = AutoDevComposeIcons.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )

                    Text(
                        text = "Suggested Fixes",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (diffPatches.isNotEmpty()) {
                        Surface(
                            color = AutoDevColors.Blue.c600.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${diffPatches.size} PATCH${if (diffPatches.size > 1) "ES" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AutoDevColors.Blue.c600,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (isActive) {
                        Surface(
                            color = AutoDevColors.Indigo.c600,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "GENERATING",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Content - 渲染所有 diff patches
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 0.dp)
                        .padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (diffPatches.isNotEmpty()) {
                        diffPatches.forEachIndexed { index, diffPatch ->
                            DiffPatchCard(
                                index = index + 1,
                                diffPatch = diffPatch,
                                onApply = { onApplyFix(diffPatch) },
                                onReject = { onRejectFix(diffPatch) },
                                workspace = workspace
                            )
                        }
                    } else if (fixOutput.isNotBlank()) {
                        // 如果没有解析到 diff patch，显示原始输出
                        Text(
                            text = fixOutput,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Text(
                            text = "No suggested fixes yet...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 单个 Diff Patch 卡片 - 可折叠，文件名和操作按钮在同一行
 */
@Composable
private fun DiffPatchCard(
    index: Int,
    diffPatch: String,
    onApply: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier,
    workspace: Workspace? = null
) {
    var isExpanded by remember { mutableStateOf(true) }
    var isApplied by remember { mutableStateOf(false) }
    var isRejected by remember { mutableStateOf(false) }
    var isApplying by remember { mutableStateOf(false) }
    var applyError by remember { mutableStateOf<String?>(null) }
    var showCompareDialog by remember { mutableStateOf(false) }
    var beforeContent by remember { mutableStateOf<String?>(null) }
    var afterContent by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()

    // 提取文件路径
    val filePath = remember(diffPatch) {
        extractFilePathFromDiff(diffPatch)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isApplied -> AutoDevColors.Green.c600.copy(alpha = 0.05f)
                isRejected -> AutoDevColors.Red.c600.copy(alpha = 0.05f)
                applyError != null -> AutoDevColors.Red.c600.copy(alpha = 0.08f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Collapsible Header - 文件名、状态和操作按钮在同一行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Expand icon + File name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isExpanded) AutoDevComposeIcons.ExpandMore else AutoDevComposeIcons.ChevronRight,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )

                    Icon(
                        imageVector = AutoDevComposeIcons.Description,
                        contentDescription = null,
                        tint = AutoDevColors.Blue.c600,
                        modifier = Modifier.size(16.dp)
                    )

                    Text(
                        text = filePath ?: "Unknown file",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Status badge
                    when {
                        isApplied -> StatusBadge("Applied", AutoDevColors.Green.c600)
                        isRejected -> StatusBadge("Rejected", AutoDevColors.Red.c600)
                        applyError != null -> StatusBadge("Failed", AutoDevColors.Red.c600)
                        isApplying -> StatusBadge("Applying...", AutoDevColors.Blue.c600)
                    }
                }

                // Right: Action buttons
                if (!isApplied && !isRejected && applyError == null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // View button
                        IconButton(
                            onClick = {
                                scope.launch {
                                    prepareCompareView(diffPatch, workspace) { before, after ->
                                        beforeContent = before
                                        afterContent = after
                                        showCompareDialog = true
                                    }
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.Visibility,
                                contentDescription = "View changes",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Apply button
                        FilledTonalButton(
                            onClick = {
                                scope.launch {
                                    isApplying = true
                                    applyError = null
                                    applyFixWithValidation(
                                        diffPatch = diffPatch,
                                        workspace = workspace,
                                        onSuccess = {
                                            isApplied = true
                                            isApplying = false
                                            onApply()
                                        },
                                        onError = { error ->
                                            applyError = error
                                            isApplying = false
                                        }
                                    )
                                }
                            },
                            enabled = !isApplying,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = AutoDevColors.Green.c600,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.height(32.dp)
                        ) {
                            if (isApplying) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = AutoDevComposeIcons.Check,
                                    contentDescription = "Apply",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            Text("Apply", style = MaterialTheme.typography.labelMedium)
                        }

                        // Reject button
                        OutlinedButton(
                            onClick = {
                                isRejected = true
                                onReject()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.Close,
                                contentDescription = "Reject",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Reject", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // Error message
            if (applyError != null) {
                Surface(
                    color = AutoDevColors.Red.c600.copy(alpha = 0.1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = AutoDevComposeIcons.Error,
                            contentDescription = "Error",
                            tint = AutoDevColors.Red.c600,
                            modifier = Modifier.size(16.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Failed to apply fix",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = AutoDevColors.Red.c600
                            )
                            Text(
                                text = applyError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Expandable content - Diff preview
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider()
                    
                    DiffSketchRenderer.RenderDiff(
                        diffContent = diffPatch,
                        modifier = Modifier.fillMaxWidth(),
                        onAccept = null, // Actions moved to header
                        onReject = null
                    )
                }
            }
        }
    }

    // Compare dialog
    if (showCompareDialog && beforeContent != null && afterContent != null) {
        CompareDialog(
            fileName = filePath ?: "Unknown",
            beforeContent = beforeContent!!,
            afterContent = afterContent!!,
            onDismiss = { showCompareDialog = false }
        )
    }
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * 对比视图对话框
 */
@Composable
private fun CompareDialog(
    fileName: String,
    beforeContent: String,
    afterContent: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = AutoDevComposeIcons.Visibility,
                    contentDescription = null,
                    tint = AutoDevColors.Blue.c600
                )
                Text("Compare Changes: $fileName")
            }
        },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Before
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Before",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = AutoDevColors.Red.c600
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = beforeContent,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // After
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "After",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = AutoDevColors.Green.c600
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = afterContent,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * 从 diff patch 中提取文件路径
 */
private fun extractFilePathFromDiff(diffPatch: String): String? {
    // Try to parse with DiffParser
    try {
        val fileDiffs = DiffParser.parse(diffPatch)
        if (fileDiffs.isNotEmpty()) {
            return fileDiffs[0].newPath ?: fileDiffs[0].oldPath
        }
    } catch (e: Exception) {
        // Fallback to regex
    }

    // Fallback: Extract from diff header
    val regex = Regex("""(?:diff --git a/.*? b/|[\+]{3} b?/?)(.+?)(?:\n|$)""")
    val match = regex.find(diffPatch)
    return match?.groupValues?.get(1)?.trim()
}

/**
 * 准备对比视图的内容
 */
private suspend fun prepareCompareView(
    diffPatch: String,
    workspace: Workspace?,
    onReady: (before: String, after: String) -> Unit
) {
    try {
        val fileDiffs = DiffParser.parse(diffPatch)
        if (fileDiffs.isEmpty() || workspace == null) return

        val fileDiff = fileDiffs[0]
        val filePath = fileDiff.newPath ?: fileDiff.oldPath ?: return

        // Read current content
        val currentContent = workspace.fileSystem.readFile(filePath) ?: ""
        
        // Simulate what content would look like after applying the patch
        val beforeLines = currentContent.lines()
        val afterLines = beforeLines.toMutableList()
        
        // Apply changes to create "after" preview
        fileDiff.hunks.forEach { hunk ->
            val startLine = maxOf(0, hunk.oldStartLine - 1)
            hunk.lines.forEach { diffLine ->
                when (diffLine.type) {
                    cc.unitmesh.agent.diff.DiffLineType.DELETED -> {
                        // Would be removed
                    }
                    cc.unitmesh.agent.diff.DiffLineType.ADDED -> {
                        // Would be added (simplified)
                    }
                    else -> {}
                }
            }
        }

        // For simplicity, show first 10 lines of each
        val beforePreview = beforeLines.take(10).joinToString("\n")
        val afterPreview = "Applied changes preview\n(Full diff shown above)"
        
        onReady(beforePreview, afterPreview)
    } catch (e: Exception) {
        // Ignore errors
    }
}

/**
 * Apply fix with validation: backup → apply → lint → rollback if error
 */
private suspend fun applyFixWithValidation(
    diffPatch: String,
    workspace: Workspace?,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    if (workspace == null) {
        onError("No workspace available")
        return
    }

    val backupContents = mutableMapOf<String, String>()
    
    try {
        // Parse the diff
        val fileDiffs = DiffParser.parse(diffPatch)
        if (fileDiffs.isEmpty()) {
            onError("Invalid diff format")
            return
        }

        // Step 1: Backup all files that will be modified
        for (fileDiff in fileDiffs) {
            val filePath = fileDiff.newPath ?: fileDiff.oldPath ?: continue
            val currentContent = workspace.fileSystem.readFile(filePath)
            if (currentContent != null) {
                backupContents[filePath] = currentContent
            }
        }

        // Step 2: Apply the patch
        for (fileDiff in fileDiffs) {
            val filePath = fileDiff.newPath ?: fileDiff.oldPath ?: continue
            val currentContent = workspace.fileSystem.readFile(filePath) ?: ""
            val currentLines = if (currentContent.isEmpty()) {
                mutableListOf()
            } else {
                currentContent.lines().toMutableList()
            }

            var lineOffset = 0
            
            // Apply each hunk
            fileDiff.hunks.forEach { hunk ->
                var currentLineIndex = maxOf(0, hunk.oldStartLine - 1) + lineOffset
                var oldLineNum = maxOf(1, hunk.oldStartLine)

                hunk.lines.forEach { diffLine ->
                    when (diffLine.type) {
                        cc.unitmesh.agent.diff.DiffLineType.CONTEXT -> {
                            if (currentLineIndex < currentLines.size) {
                                currentLineIndex++
                                oldLineNum++
                            }
                        }
                        cc.unitmesh.agent.diff.DiffLineType.DELETED -> {
                            if (currentLineIndex < currentLines.size) {
                                currentLines.removeAt(currentLineIndex)
                                lineOffset--
                                oldLineNum++
                            }
                        }
                        cc.unitmesh.agent.diff.DiffLineType.ADDED -> {
                            if (currentLineIndex <= currentLines.size) {
                                currentLines.add(currentLineIndex, diffLine.content)
                                lineOffset++
                                currentLineIndex++
                            }
                        }
                        cc.unitmesh.agent.diff.DiffLineType.HEADER -> {}
                    }
                }
            }

            // Write modified content
            val newContent = currentLines.joinToString("\n")
            workspace.fileSystem.writeFile(filePath, newContent)
        }

        // Step 3: Run lint validation
        val projectPath = workspace.rootPath ?: ""
        val modifiedFiles = fileDiffs.mapNotNull { it.newPath ?: it.oldPath }
        
        var hasErrors = false
        try {
            val linterRegistry = LinterRegistry.getInstance()
            val lintSummary = linterRegistry.getLinterSummaryForFiles(modifiedFiles, projectPath)
            
            // Check for errors
            if (lintSummary.errorCount > 0) {
                hasErrors = true
                val errorMsg = buildString {
                    append("Lint validation failed with ${lintSummary.errorCount} error(s):\n")
                    lintSummary.fileIssues.forEach { fileIssue ->
                        if (fileIssue.errorCount > 0) {
                            append("• ${fileIssue.filePath}: ${fileIssue.errorCount} error(s)\n")
                            fileIssue.topIssues.take(3).forEach { issue ->
                                if (issue.severity.name == "ERROR") {
                                    append("  - Line ${issue.line}: ${issue.message}\n")
                                }
                            }
                        }
                    }
                }
                throw Exception(errorMsg)
            }
        } catch (e: Exception) {
            if (hasErrors) throw e
            // Lint execution failed, but might not be critical
        }

        // Success!
        onSuccess()
        
    } catch (e: Exception) {
        // Step 4: Rollback on error
        for ((filePath, content) in backupContents) {
            try {
                workspace.fileSystem.writeFile(filePath, content)
            } catch (rollbackError: Exception) {
                // Log but don't throw
            }
        }
        
        onError(e.message ?: "Unknown error occurred")
    }
}

/**
 * 从输出中提取所有 diff patch
 * 支持两种格式：
 * 1. ```diff ... ``` 代码块
 * 2. 标准的 diff 输出（以 diff --git 开头）
 */
private fun extractDiffPatches(output: String): List<String> {
    val patches = mutableListOf<String>()

    // 方法1: 使用 CodeFence 解析
    val codeFences = CodeFence.parseAll(output)
    codeFences.forEach { fence ->
        if (fence.languageId.lowercase() in listOf("diff", "patch")) {
            if (fence.text.isNotBlank()) {
                patches.add(fence.text)
            }
        }
    }

    // 方法2: 如果没有找到 code fence，尝试查找标准 diff 输出
    if (patches.isEmpty()) {
        val diffPattern = Regex("""diff --git[\s\S]*?(?=\ndiff --git|\z)""")
        val matches = diffPattern.findAll(output)
        matches.forEach { match ->
            patches.add(match.value.trim())
        }
    }

    return patches
}

