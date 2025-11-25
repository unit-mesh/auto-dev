package cc.unitmesh.devins.ui.compose.document

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.document.DocumentFile
import cc.unitmesh.devins.document.DocumentFormatType
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons

/**
 * 文档查看面板 - 中间上部
 * 用于显示文档内容
 * 
 * @param rawContent 原始文件内容（用于 Markdown 等文本格式）
 * @param parsedContent 解析后的内容（用于 PDF/Word/Excel 等，由 Tika 提取）
 */
@Composable
fun DocumentViewerPane(
    document: DocumentFile?,
    rawContent: String?,
    parsedContent: String?,
    isLoading: Boolean,
    indexStatus: cc.unitmesh.devins.db.DocumentIndexRecord? = null,
    targetLineNumber: Int? = null,
    highlightedText: String? = null,
    modifier: Modifier = Modifier
) {
    // Determine which content to display based on document type
    val displayContent = when {
        parsedContent != null -> parsedContent  // Use parsed content for PDF/Word/etc.
        rawContent != null -> rawContent        // Use raw content for Markdown/Text
        else -> null
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (document == null) {
            EmptyViewerState()
        } else {
            Text(
                text = document.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MetadataItem(
                    icon = AutoDevComposeIcons.Schedule,
                    text = "Last modified: ${formatDate(document.metadata.lastModified)}"
                )

                if (document.metadata.fileSize > 0) {
                    MetadataItem(
                        icon = AutoDevComposeIcons.Description,
                        text = formatFileSize(document.metadata.fileSize)
                    )
                }

                if (indexStatus != null) {
                    val statusColor = if (indexStatus.status == "INDEXED")
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error

                    val statusText = if (indexStatus.status == "INDEXED") "Indexed" else "Index Failed"
                    val statusIcon = if (indexStatus.status == "INDEXED")
                        AutoDevComposeIcons.CheckCircle
                    else
                        AutoDevComposeIcons.Error

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = statusColor
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = AutoDevComposeIcons.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Not Indexed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))

            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (displayContent != null) {
                    // Show format indicator for parsed content
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (parsedContent != null && document != null) {
                            DocumentFormatIndicator(
                                formatType = document.metadata.formatType,
                                isParsed = true
                            )
                        }
                        
                        DocumentContentView(
                            content = displayContent,
                            targetLineNumber = targetLineNumber,
                            highlightedText = highlightedText,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Text(
                        text = "无法加载文档内容",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

/**
 * Document content view with support for scrolling to specific lines and highlighting
 */
@Composable
private fun DocumentContentView(
    content: String,
    targetLineNumber: Int?,
    highlightedText: String?,
    modifier: Modifier = Modifier
) {
    val lines = remember(content) { content.lines() }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    // Scroll to target line when it changes
    androidx.compose.runtime.LaunchedEffect(targetLineNumber) {
        if (targetLineNumber != null && targetLineNumber > 0 && targetLineNumber <= lines.size) {
            // Scroll to the target line (0-indexed)
            listState.animateScrollToItem(
                index = (targetLineNumber - 1).coerceAtLeast(0),
                scrollOffset = 0
            )
        }
    }
    
    androidx.compose.foundation.lazy.LazyColumn(
        state = listState,
        modifier = modifier
    ) {
        items(lines.size) { index ->
            val line = lines[index]
            val shouldHighlight = highlightedText != null && line.contains(highlightedText, ignoreCase = true)
            
            Text(
                text = line.ifEmpty { " " }, // Empty lines need a space to render
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (shouldHighlight) {
                            Modifier.background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun EmptyViewerState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = AutoDevComposeIcons.MenuBook,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "选择一个文档开始阅读",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MetadataItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Document format indicator to show that content is parsed/extracted
 */
@Composable
private fun DocumentFormatIndicator(
    formatType: DocumentFormatType,
    isParsed: Boolean
) {
    if (!isParsed) return
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = AutoDevComposeIcons.Info,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Column {
                Text(
                    text = "已解析的文本内容",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = when (formatType) {
                        DocumentFormatType.PDF -> "从 PDF 文件提取的纯文本"
                        DocumentFormatType.DOCX -> "从 Word 文档提取的纯文本"
                        DocumentFormatType.PLAIN_TEXT -> "纯文本文件"
                        DocumentFormatType.MARKDOWN -> "Markdown 文档"
                        else -> "从 ${formatType.name} 提取的纯文本"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// 简单的日期格式化 (实际项目中应使用 DateTimeFormatter)
private fun formatDate(timestamp: Long): String {
    // 这里只是一个简单的占位符，实际应该用 kotlinx-datetime
    return "Recently"
}

// 文件大小格式化
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}
