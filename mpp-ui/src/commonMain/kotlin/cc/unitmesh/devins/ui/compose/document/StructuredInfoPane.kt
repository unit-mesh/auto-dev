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
import cc.unitmesh.devins.document.Entity
import cc.unitmesh.devins.document.TOCItem
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons


@Composable
fun StructuredInfoPane(
    toc: List<TOCItem>,
    entities: List<Entity>,
    onTocSelected: (TOCItem) -> Unit,
    onEntitySelected: (Entity) -> Unit,
    onDocQLQuery: suspend (String) -> cc.unitmesh.devins.document.docql.DocQLResult,
    modifier: Modifier = Modifier
) {
    var docqlResult by remember { mutableStateOf<cc.unitmesh.devins.document.docql.DocQLResult?>(null) }
    var showDocQLResult by remember { mutableStateOf(false) }
    var tocExpanded by remember { mutableStateOf(true) }
    var entitiesExpanded by remember { mutableStateOf(false) }

    // Reset DocQL result when document changes (toc or entities change)
    LaunchedEffect(toc, entities) {
        docqlResult = null
        showDocQLResult = false
        tocExpanded = true
        entitiesExpanded = false
    }

    Column(modifier = modifier.fillMaxSize()) {
        key(toc, entities) {
            DocQLSearchBar(
                onQueryExecute = { query ->
                    val result = onDocQLQuery(query)
                    docqlResult = result
                    showDocQLResult = true
                    result
                },
                autoExecute = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }

        HorizontalDivider()

        if (showDocQLResult && docqlResult != null) {
            DocQLResultView(
                result = docqlResult!!,
                onTocSelected = onTocSelected,
                onEntitySelected = onEntitySelected,
                onClose = { showDocQLResult = false }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // TOC Section
                item {
                    CollapsibleSection(
                        title = "目录",
                        count = toc.size,
                        expanded = tocExpanded,
                        onToggle = { tocExpanded = !tocExpanded },
                        icon = AutoDevComposeIcons.List
                    ) {
                        if (toc.isEmpty()) {
                            Text(
                                text = "暂无目录信息",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                toc.forEach { item ->
                                    TocItemRow(item, onTocSelected)
                                }
                            }
                        }
                    }
                }

                // Entities Section
                item {
                    CollapsibleSection(
                        title = "实体",
                        count = entities.size,
                        expanded = entitiesExpanded,
                        onToggle = { entitiesExpanded = !entitiesExpanded },
                        icon = AutoDevComposeIcons.DataObject
                    ) {
                        if (entities.isEmpty()) {
                            Text(
                                text = "未提取到关键实体",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                entities.forEach { entity ->
                                    EntityItemRow(entity, onEntitySelected)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Collapsible section component
 */
@Composable
private fun CollapsibleSection(
    title: String,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    if (count > 0) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Icon(
                    imageVector = if (expanded) AutoDevComposeIcons.ExpandLess else AutoDevComposeIcons.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Content
            if (expanded) {
                HorizontalDivider()
                Box(modifier = Modifier.padding(8.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun TocItemRow(
    item: TOCItem,
    onTocSelected: (TOCItem) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTocSelected(item) }
                .padding(
                    start = ((item.level - 1) * 16).dp,
                    top = 8.dp,
                    bottom = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (item.level == 1) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (item.page != null) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "P${item.page}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 递归显示子项
        item.children.forEach { child ->
            TocItemRow(child, onTocSelected)
        }
    }
}

@Composable
private fun EntityItemRow(
    entity: Entity,
    onEntitySelected: (Entity) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEntitySelected(entity) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 图标
            Icon(
                imageVector = when (entity) {
                    is Entity.Term -> AutoDevComposeIcons.Label
                    is Entity.API -> AutoDevComposeIcons.Api
                    is Entity.ClassEntity -> AutoDevComposeIcons.DataObject
                    is Entity.FunctionEntity -> AutoDevComposeIcons.Functions
                },
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Column {
                Text(
                    text = entity.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

                val desc = when (entity) {
                    is Entity.Term -> entity.definition
                    is Entity.API -> entity.signature
                    is Entity.ClassEntity -> "Class"
                    is Entity.FunctionEntity -> entity.signature
                }

                if (!desc.isNullOrEmpty()) {
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * DocQL 查询结果视图
 */
@Composable
private fun DocQLResultView(
    result: cc.unitmesh.devins.document.docql.DocQLResult,
    onTocSelected: (TOCItem) -> Unit,
    onEntitySelected: (Entity) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 结果头部
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "查询结果",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = AutoDevComposeIcons.Close,
                    contentDescription = "关闭结果"
                )
            }
        }

        HorizontalDivider()

        // 显示结果
        when (result) {
            is cc.unitmesh.devins.document.docql.DocQLResult.TocItems -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "找到 ${result.totalCount} 个目录项来自 ${result.itemsByFile.size} 个文件",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        result.itemsByFile.forEach { (filePath, items) ->
                            item {
                                Text(
                                    text = "📄 $filePath",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(items) { item ->
                                TocItemRow(item, onTocSelected)
                            }
                        }
                    }
                }
            }

            is cc.unitmesh.devins.document.docql.DocQLResult.Entities -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "找到 ${result.totalCount} 个实体来自 ${result.itemsByFile.size} 个文件",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        result.itemsByFile.forEach { (filePath, items) ->
                            item {
                                Text(
                                    text = "📄 $filePath",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(items) { entity ->
                                EntityItemRow(entity, onEntitySelected)
                            }
                        }
                    }
                }
            }

            is cc.unitmesh.devins.document.docql.DocQLResult.Chunks -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "找到 ${result.totalCount} 个内容块来自 ${result.itemsByFile.size} 个文件",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        result.itemsByFile.forEach { (filePath, items) ->
                            item {
                                Text(
                                    text = "📄 $filePath",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(items) { chunk ->
                                ChunkItemCard(chunk)
                            }
                        }
                    }
                }
            }

            is cc.unitmesh.devins.document.docql.DocQLResult.Empty -> {
                EmptyState("没有找到匹配的结果")
            }

            is cc.unitmesh.devins.document.docql.DocQLResult.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "错误: ${result.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            is cc.unitmesh.devins.document.docql.DocQLResult.CodeBlocks -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "找到 ${result.totalCount} 个代码块来自 ${result.itemsByFile.size} 个文件",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        result.itemsByFile.forEach { (filePath, items) ->
                            item {
                                Text(
                                    text = "📄 $filePath (${items.size} 个代码块)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            is cc.unitmesh.devins.document.docql.DocQLResult.Tables -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "找到 ${result.totalCount} 个表格来自 ${result.itemsByFile.size} 个文件",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        result.itemsByFile.forEach { (filePath, items) ->
                            item {
                                Text(
                                    text = "📄 $filePath (${items.size} 个表格)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            is cc.unitmesh.devins.document.docql.DocQLResult.Files -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "找到 ${result.items.size} 个文件",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(result.items) { file ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (file.directory.isNotEmpty()) {
                                        Text(
                                            text = file.directory,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChunkItemCard(chunk: cc.unitmesh.devins.document.DocumentChunk) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chunk.chapterTitle?.let { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

            Text(
                text = chunk.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )

            // Position metadata display
            chunk.position?.let { position ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = AutoDevComposeIcons.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = position.toLocationString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    // Format type badge
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = position.formatType.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            } ?: run {
                // Fallback to legacy fields if position metadata not available
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (chunk.page != null) {
                        Text(
                            text = "Page ${chunk.page}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (chunk.startLine != null) {
                        val lineText = if (chunk.endLine != null && chunk.endLine != chunk.startLine) {
                            "Lines ${chunk.startLine}-${chunk.endLine}"
                        } else {
                            "Line ${chunk.startLine}"
                        }
                        Text(
                            text = lineText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
