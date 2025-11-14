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
import cc.unitmesh.devins.parser.CodeFence
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.compose.sketch.DiffSketchRenderer
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors

/**
 * Suggested Fixes Section - 展示 AI 建议的修复（Diff Patch 格式）
 * 支持 accept/reject 操作
 */
@Composable
fun SuggestedFixesSection(
    fixOutput: String,
    isActive: Boolean,
    onApplyFix: (String) -> Unit,
    onRejectFix: (String) -> Unit,
    modifier: Modifier = Modifier
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
                                onReject = { onRejectFix(diffPatch) }
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
 * 单个 Diff Patch 卡片
 */
@Composable
private fun DiffPatchCard(
    index: Int,
    diffPatch: String,
    onApply: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isApplied by remember { mutableStateOf(false) }
    var isRejected by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isApplied -> AutoDevColors.Green.c600.copy(alpha = 0.05f)
                isRejected -> AutoDevColors.Red.c600.copy(alpha = 0.05f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Patch header with status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fix #$index",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                when {
                    isApplied -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.CheckCircle,
                                contentDescription = "Applied",
                                tint = AutoDevColors.Green.c600,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Applied",
                                style = MaterialTheme.typography.labelSmall,
                                color = AutoDevColors.Green.c600
                            )
                        }
                    }
                    isRejected -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.Close,
                                contentDescription = "Rejected",
                                tint = AutoDevColors.Red.c600,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Rejected",
                                style = MaterialTheme.typography.labelSmall,
                                color = AutoDevColors.Red.c600
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Render the diff patch
            DiffSketchRenderer.RenderDiff(
                diffContent = diffPatch,
                modifier = Modifier.fillMaxWidth(),
                onAccept = if (!isApplied && !isRejected) {
                    {
                        isApplied = true
                        onApply()
                    }
                } else null,
                onReject = if (!isApplied && !isRejected) {
                    {
                        isRejected = true
                        onReject()
                    }
                } else null
            )
        }
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

