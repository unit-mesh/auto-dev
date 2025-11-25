package cc.unitmesh.devins.ui.compose.document

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.document.DocumentFile
import cc.unitmesh.devins.document.DocumentFolder
import cc.unitmesh.devins.document.DocumentTreeNode
import cc.unitmesh.devins.document.ParseStatus
import cc.unitmesh.devins.service.IndexingStatus

@Composable
fun DocumentNavigationPane(
    documentLoadState: DocumentLoadState = DocumentLoadState.Initial,
    documents: List<DocumentFile> = emptyList(),
    indexingStatus: IndexingStatus = IndexingStatus.Idle,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onDocumentSelected: (DocumentFile) -> Unit = {},
    onRefresh: () -> Unit = {},
    onStartIndexing: () -> Unit = {},
    onResetIndexing: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Top toolbar with indexing status and search
        DocumentNavigationToolbar(
            indexingStatus = indexingStatus,
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            onRefresh = onRefresh,
            onStartIndexing = onStartIndexing,
            onResetIndexing = onResetIndexing,
            documentsLoaded = documentLoadState is DocumentLoadState.Success
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Content based on load state
        when (documentLoadState) {
            is DocumentLoadState.Initial -> {
                // Nothing to show yet
            }
            is DocumentLoadState.Loading -> {
                LoadingDocumentsState()
            }
            is DocumentLoadState.Empty -> {
                EmptyDocumentState()
            }
            is DocumentLoadState.Error -> {
                ErrorDocumentState(documentLoadState.message)
            }
            is DocumentLoadState.Success -> {
                if (documents.isEmpty()) {
                    // Filtered results are empty
                    NoSearchResultsState()
                } else {
                    DocumentTree(
                        documents = documents,
                        onDocumentSelected = onDocumentSelected
                    )
                }
            }
        }
    }
}

/**
 * Document navigation toolbar with indexing button and search
 */
@Composable
private fun DocumentNavigationToolbar(
    indexingStatus: IndexingStatus,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onStartIndexing: () -> Unit,
    onResetIndexing: () -> Unit,
    documentsLoaded: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Indexing status/button
        when (indexingStatus) {
            is IndexingStatus.Idle -> {
                // Show "Index Documents" button
                Button(
                    onClick = onStartIndexing,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = documentsLoaded
                ) {
                    Icon(
                        imageVector = AutoDevComposeIcons.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("索引文档")
                }
            }
            is IndexingStatus.Indexing -> {
                // Show progress indicator
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "索引中...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "${indexingStatus.current}/${indexingStatus.total}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        // Progress bar
                        LinearProgressIndicator(
                            progress = { 
                                if (indexingStatus.total > 0) {
                                    indexingStatus.current.toFloat() / indexingStatus.total
                                } else {
                                    0f
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Success/Failed stats
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = AutoDevComposeIcons.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${indexingStatus.succeeded}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            if (indexingStatus.failed > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = AutoDevComposeIcons.Error,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "${indexingStatus.failed}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
            is IndexingStatus.Completed -> {
                // Show completion summary with reset button
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (indexingStatus.failed > 0) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.tertiaryContainer
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "索引完成",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "成功: ${indexingStatus.succeeded}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                if (indexingStatus.failed > 0) {
                                    Text(
                                        text = "失败: ${indexingStatus.failed}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        IconButton(onClick = onResetIndexing) {
                            Icon(
                                imageVector = AutoDevComposeIcons.Close,
                                contentDescription = "关闭",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Search field
        val searchEnabled = indexingStatus is IndexingStatus.Completed
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                val placeholderText = when {
                    !documentsLoaded -> "加载文档后可搜索..."
                    indexingStatus !is IndexingStatus.Completed -> "索引完成后可搜索内容..."
                    else -> "搜索文档（文件名和内容）..."
                }
                Text(
                    text = placeholderText,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = AutoDevComposeIcons.Search,
                    contentDescription = "Search"
                )
            },
            trailingIcon = {
                Row {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = AutoDevComposeIcons.Close,
                                contentDescription = "Clear search",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = AutoDevComposeIcons.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            enabled = searchEnabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun LoadingDocumentsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "加载文档中...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyDocumentState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = AutoDevComposeIcons.Article,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无文档",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "从项目中添加文档开始使用",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ErrorDocumentState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = AutoDevComposeIcons.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "加载失败",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun NoSearchResultsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = AutoDevComposeIcons.Search,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "未找到匹配的文档",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "尝试使用其他关键词",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

/**
 * 文档树显示
 */
@Composable
private fun DocumentTree(
    documents: List<DocumentFile>,
    onDocumentSelected: (DocumentFile) -> Unit
) {
    // 构建树形结构
    val treeNodes = remember(documents) {
        buildDocumentTreeStructure(documents)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(treeNodes) { node ->
            DocumentTreeNodeItem(
                node = node,
                level = 0,
                onDocumentSelected = onDocumentSelected
            )
        }
    }
}

/**
 * 文档树节点项
 */
@Composable
private fun DocumentTreeNodeItem(
    node: DocumentTreeNode,
    level: Int,
    onDocumentSelected: (DocumentFile) -> Unit
) {
    when (node) {
        is DocumentFolder -> {
            DocumentFolderItem(
                folder = node,
                level = level,
                onDocumentSelected = onDocumentSelected
            )
        }
        is DocumentFile -> {
            DocumentFileItem(
                file = node,
                level = level,
                onDocumentSelected = onDocumentSelected
            )
        }
    }
}

/**
 * 文件夹项（可展开/折叠）
 */
@Composable
private fun DocumentFolderItem(
    folder: DocumentFolder,
    level: Int,
    onDocumentSelected: (DocumentFile) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }

    Column {
        // 文件夹头部
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(
                    start = (level * 16 + 8).dp,
                    top = 4.dp,
                    bottom = 4.dp,
                    end = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isExpanded) {
                    AutoDevComposeIcons.KeyboardArrowDown
                } else {
                    AutoDevComposeIcons.KeyboardArrowRight
                },
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Icon(
                imageVector = if (isExpanded) {
                    AutoDevComposeIcons.FolderOpen
                } else {
                    AutoDevComposeIcons.Folder
                },
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = folder.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 文件数量徽章
            if (folder.fileCount > 0) {
                Text(
                    text = folder.fileCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        // 子节点
        if (isExpanded && folder.children.isNotEmpty()) {
            folder.children.forEach { child ->
                DocumentTreeNodeItem(
                    node = child,
                    level = level + 1,
                    onDocumentSelected = onDocumentSelected
                )
            }
        }
    }
}

/**
 * 文档文件项
 */
@Composable
private fun DocumentFileItem(
    file: DocumentFile,
    level: Int,
    onDocumentSelected: (DocumentFile) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDocumentSelected(file) }
            .padding(
                start = (level * 16 + 24).dp,
                top = 6.dp,
                bottom = 6.dp,
                end = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = AutoDevComposeIcons.Article,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.secondary
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (file.metadata.chapterCount > 0 || file.metadata.totalPages != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (file.metadata.totalPages != null) {
                        Text(
                            text = "${file.metadata.totalPages} 页",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    if (file.metadata.chapterCount > 0) {
                        Text(
                            text = "${file.metadata.chapterCount} 章节",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        when (file.metadata.parseStatus) {
            ParseStatus.PARSED -> {
                Icon(
                    imageVector = AutoDevComposeIcons.CheckCircle,
                    contentDescription = "Parsed",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            ParseStatus.PARSING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp
                )
            }
            ParseStatus.PARSE_FAILED -> {
                Icon(
                    imageVector = AutoDevComposeIcons.Error,
                    contentDescription = "Parse Failed",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
            else -> {}
        }
    }
}

/**
 * 构建文档树结构
 * 类似 DiffFileTreeView 的 buildFileTreeStructure
 */
fun buildDocumentTreeStructure(documents: List<DocumentFile>): List<DocumentTreeNode> {
    val rootNodes = mutableListOf<DocumentTreeNode>()
    val folderMap = mutableMapOf<String, DocumentFolder>()

    documents.forEach { doc ->
        val parts = doc.path.split("/")
        var currentPath = ""
        var parentFolder: DocumentFolder? = null

        for (i in 0 until parts.size - 1) {
            val part = parts[i]
            currentPath = if (currentPath.isEmpty()) part else "$currentPath/$part"

            var folder = folderMap[currentPath]
            if (folder == null) {
                folder = DocumentFolder(part, currentPath)
                folderMap[currentPath] = folder

                if (parentFolder != null) {
                    parentFolder.children.add(folder)
                } else {
                    rootNodes.add(folder)
                }
            }
            parentFolder = folder
        }

        if (parentFolder != null) {
            parentFolder.children.add(doc)
        } else {
            rootNodes.add(doc)
        }
    }

    // Post-process counts
    rootNodes.forEach { updateFileCount(it) }

    return rootNodes
}

private fun updateFileCount(node: DocumentTreeNode): Int {
    return when (node) {
        is DocumentFile -> 1
        is DocumentFolder -> {
            var count = 0
            node.children.forEach { count += updateFileCount(it) }
            (node as DocumentFolder).fileCount = count
            count
        }
    }
}
