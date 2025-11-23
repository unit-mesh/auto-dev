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

/**
 * 结构化信息面板 - 中间下部
 * 包含 TOC (目录)、Graph (关系图)、Entities (实体列表) 和 DocQL 查询
 */
@Composable
fun StructuredInfoPane(
    toc: List<TOCItem>,
    entities: List<Entity>,
    onTocSelected: (TOCItem) -> Unit,
    onEntitySelected: (Entity) -> Unit,
    onDocQLQuery: suspend (String) -> cc.unitmesh.devins.document.docql.DocQLResult,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    var docqlResult by remember { mutableStateOf<cc.unitmesh.devins.document.docql.DocQLResult?>(null) }
    val tabs = listOf("目录", "关系图", "实体", "DocQL")

    Column(modifier = modifier.fillMaxSize()) {
        // Tab 栏
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = { HorizontalDivider() }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        // 内容区域
        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            when (selectedTabIndex) {
                0 -> TocView(toc, onTocSelected)
                1 -> GraphView()
                2 -> EntityListView(entities, onEntitySelected)
                3 -> DocQLView(
                    onQueryExecute = { query ->
                        val result = onDocQLQuery(query)
                        docqlResult = result
                        result
                    },
                    result = docqlResult,
                    onTocSelected = onTocSelected,
                    onEntitySelected = onEntitySelected
                )
            }
        }
    }
}

@Composable
private fun TocView(
    toc: List<TOCItem>,
    onTocSelected: (TOCItem) -> Unit
) {
    if (toc.isEmpty()) {
        EmptyState("暂无目录信息")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(toc) { item ->
                TocItemRow(item, onTocSelected)
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
private fun GraphView() {
    EmptyState("文档关系图生成中...")
}

@Composable
private fun EntityListView(
    entities: List<Entity>,
    onEntitySelected: (Entity) -> Unit
) {
    if (entities.isEmpty()) {
        EmptyState("未提取到关键实体")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(entities) { entity ->
                EntityItemRow(entity, onEntitySelected)
            }
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

@Composable
private fun DocQLView(
    onQueryExecute: suspend (String) -> cc.unitmesh.devins.document.docql.DocQLResult,
    result: cc.unitmesh.devins.document.docql.DocQLResult?,
    onTocSelected: (TOCItem) -> Unit,
    onEntitySelected: (Entity) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Search bar
        DocQLSearchBar(
            onQueryExecute = onQueryExecute,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Results
        if (result != null) {
            when (result) {
                is cc.unitmesh.devins.document.docql.DocQLResult.TocItems -> {
                    Text(
                        text = "Found ${result.items.size} TOC items",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(result.items) { item ->
                            TocItemRow(item, onTocSelected)
                        }
                    }
                }
                
                is cc.unitmesh.devins.document.docql.DocQLResult.Entities -> {
                    Text(
                        text = "Found ${result.items.size} entities",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(result.items) { entity ->
                            EntityItemRow(entity, onEntitySelected)
                        }
                    }
                }
                
                is cc.unitmesh.devins.document.docql.DocQLResult.Chunks -> {
                    Text(
                        text = "Found ${result.items.size} chunks",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(result.items) { chunk ->
                            ChunkItemCard(chunk)
                        }
                    }
                }
                
                is cc.unitmesh.devins.document.docql.DocQLResult.Empty -> {
                    EmptyState("No results found")
                }
                
                is cc.unitmesh.devins.document.docql.DocQLResult.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "Error: ${result.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                
                is cc.unitmesh.devins.document.docql.DocQLResult.CodeBlocks -> {
                    Text(
                        text = "Found ${result.items.size} code blocks",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    EmptyState("Code block display not implemented yet")
                }
                
                is cc.unitmesh.devins.document.docql.DocQLResult.Tables -> {
                    Text(
                        text = "Found ${result.items.size} tables",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    EmptyState("Table display not implemented yet")
                }
            }
        } else {
            EmptyState("Enter a DocQL query to search the document")
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
                        fontWeight = FontWeight.Medium
                    )
                }
            
            Text(
                text = chunk.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
            
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
                    Text(
                        text = "Line ${chunk.startLine}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
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
