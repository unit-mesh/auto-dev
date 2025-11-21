package cc.unitmesh.devins.ui.compose.agent.codereview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cc.unitmesh.agent.diff.DiffParser
import cc.unitmesh.agent.diff.DiffUtils
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
                                workspace = workspace,
                                isGenerating = isActive  // 传递生成状态
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
    workspace: Workspace? = null,
    isGenerating: Boolean = false  // 是否正在流式生成
) {
    var isExpanded by remember { mutableStateOf(true) }
    var isApplied by remember { mutableStateOf(false) }
    var isRejected by remember { mutableStateOf(false) }
    var isApplying by remember { mutableStateOf(false) }
    var applyError by remember { mutableStateOf<String?>(null) }
    var showCompareDialog by remember { mutableStateOf(false) }
    var diffContentForDialog by remember { mutableStateOf<String?>(null) }
    var filePathForDialog by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()

    // 提取文件路径
    val filePath = remember(diffPatch) {
        extractFilePathFromDiff(diffPatch)
    }
    
    // 检查 patch 是否完整（用于流式生成时禁用按钮）
    val isPatchComplete = remember(diffPatch, isGenerating) {
        !isGenerating && isDiffPatchComplete(diffPatch)
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
                        !isPatchComplete -> StatusBadge("Generating...", AutoDevColors.Amber.c600)
                    }
                }

                // Right: Action buttons
                if (!isApplied && !isRejected && applyError == null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // View button - 只在 patch 完整时启用
                        IconButton(
                            onClick = {
                                scope.launch {
                                    prepareDiffForDialog(diffPatch, workspace) { diff, path ->
                                        diffContentForDialog = diff
                                        filePathForDialog = path
                                        showCompareDialog = true
                                    }
                                }
                            },
                            enabled = isPatchComplete,  // 生成中禁用
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.Visibility,
                                contentDescription = "View changes",
                                tint = if (isPatchComplete) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Apply button - 只在 patch 完整时启用
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
                            enabled = isPatchComplete && !isApplying,  // 生成中或应用中禁用
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = AutoDevColors.Green.c600,
                                contentColor = Color.White,
                                disabledContainerColor = AutoDevColors.Green.c600.copy(alpha = 0.3f),
                                disabledContentColor = Color.White.copy(alpha = 0.5f)
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

                        // Reject button - 只在 patch 完整时启用
                        OutlinedButton(
                            onClick = {
                                isRejected = true
                                onReject()
                            },
                            enabled = isPatchComplete,  // 生成中禁用
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
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
    if (showCompareDialog && diffContentForDialog != null) {
        CompareDialog(
            fileName = filePathForDialog ?: filePath ?: "Unknown",
            diffContent = diffContentForDialog!!,
            onDismiss = { 
                showCompareDialog = false
                diffContentForDialog = null
                filePathForDialog = null
            }
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
 * 对比视图对话框 - 参考 DiffViewDialog 的实现
 */
@Composable
private fun CompareDialog(
    fileName: String,
    diffContent: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 600.dp, max = 1200.dp)
                .heightIn(min = 400.dp, max = 900.dp)
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.Visibility,
                                contentDescription = null,
                                tint = AutoDevColors.Blue.c600,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Compare Changes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = AutoDevComposeIcons.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                HorizontalDivider()

                // Diff content with scroll support
                val verticalScrollState = rememberScrollState()
                val horizontalScrollState = rememberScrollState()
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(verticalScrollState)
                        .horizontalScroll(horizontalScrollState)
                        .padding(16.dp)
                ) {
                    if (diffContent.isNotBlank()) {
                        DiffSketchRenderer.RenderDiff(
                            diffContent = diffContent,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = "No changes to display",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * 检查 diff patch 是否完整（可以安全应用）
 */
private fun isDiffPatchComplete(diffPatch: String): Boolean {
    if (diffPatch.isBlank()) return false
    
    try {
        // 尝试用 DiffParser 解析
        val fileDiffs = DiffParser.parse(diffPatch)
        
        // 检查是否有至少一个文件和一个 hunk
        if (fileDiffs.isEmpty()) return false
        
        val firstDiff = fileDiffs[0]
        
        // 必须有文件路径和至少一个 hunk
        if ((firstDiff.newPath == null && firstDiff.oldPath == null) || firstDiff.hunks.isEmpty()) {
            return false
        }
        
        // 检查每个 hunk 是否有实际的变更内容
        firstDiff.hunks.forEach { hunk ->
            if (hunk.lines.isEmpty()) return false
            
            // 至少应该有一个添加或删除的行
            val hasChanges = hunk.lines.any { 
                it.type == cc.unitmesh.agent.diff.DiffLineType.ADDED || 
                it.type == cc.unitmesh.agent.diff.DiffLineType.DELETED 
            }
            if (!hasChanges) return false
        }
        
        return true
    } catch (e: Exception) {
        // 解析失败说明 patch 不完整或格式错误
        return false
    }
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
 * 准备对话框的 diff 内容 - 应用 patch 并生成完整的 unified diff
 */
private suspend fun prepareDiffForDialog(
    diffPatch: String,
    workspace: Workspace?,
    onReady: (diffContent: String, filePath: String) -> Unit
) {
    try {
        val fileDiffs = DiffParser.parse(diffPatch)
        if (fileDiffs.isEmpty() || workspace == null) return

        val fileDiff = fileDiffs[0]
        val filePath = fileDiff.newPath ?: fileDiff.oldPath ?: return

        // Read current file content (before)
        val beforeContent = workspace.fileSystem.readFile(filePath) ?: ""
        
        // Apply patch to get after content
        val beforeLines = if (beforeContent.isEmpty()) {
            mutableListOf()
        } else {
            beforeContent.lines().toMutableList()
        }
        
        val afterLines = beforeLines.toMutableList()
        var lineOffset = 0
        
        // Apply each hunk to create the "after" content
        fileDiff.hunks.forEach { hunk ->
            var currentLineIndex = maxOf(0, hunk.oldStartLine - 1) + lineOffset

            hunk.lines.forEach { diffLine ->
                when (diffLine.type) {
                    cc.unitmesh.agent.diff.DiffLineType.CONTEXT -> {
                        if (currentLineIndex < afterLines.size) {
                            currentLineIndex++
                        }
                    }
                    cc.unitmesh.agent.diff.DiffLineType.DELETED -> {
                        if (currentLineIndex < afterLines.size) {
                            afterLines.removeAt(currentLineIndex)
                            lineOffset--
                        }
                    }
                    cc.unitmesh.agent.diff.DiffLineType.ADDED -> {
                        if (currentLineIndex <= afterLines.size) {
                            afterLines.add(currentLineIndex, diffLine.content)
                            lineOffset++
                            currentLineIndex++
                        }
                    }
                    cc.unitmesh.agent.diff.DiffLineType.HEADER -> {}
                }
            }
        }

        // Generate unified diff from before and after
        val afterContent = afterLines.joinToString("\n")
        val unifiedDiff = DiffUtils.generateUnifiedDiff(
            beforeContent,
            afterContent,
            filePath
        )
        
        onReady(unifiedDiff, filePath)
    } catch (e: Exception) {
        // Ignore errors - dialog won't show
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

