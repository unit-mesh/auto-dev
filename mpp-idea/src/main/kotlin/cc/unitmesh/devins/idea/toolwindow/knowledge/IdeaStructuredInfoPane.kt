package cc.unitmesh.devins.idea.toolwindow.knowledge

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.document.Entity
import cc.unitmesh.devins.document.TOCItem
import cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.*

/**
 * Structured information pane for displaying TOC and entities.
 * Adapted from mpp-ui's StructuredInfoPane for Jewel theme.
 */
@Composable
fun IdeaStructuredInfoPane(
    toc: List<TOCItem>,
    entities: List<Entity>,
    onTocSelected: (TOCItem) -> Unit,
    onEntitySelected: (Entity) -> Unit,
    modifier: Modifier = Modifier
) {
    var tocExpanded by remember { mutableStateOf(true) }
    var entitiesExpanded by remember { mutableStateOf(false) }

    // Reset expansion state when content changes
    LaunchedEffect(toc, entities) {
        tocExpanded = true
        entitiesExpanded = false
    }

    Column(modifier = modifier.fillMaxSize().padding(8.dp)) {
        // TOC Section
        IdeaCollapsibleSection(
            title = "Table of Contents",
            count = toc.size,
            expanded = tocExpanded,
            onToggle = { tocExpanded = !tocExpanded },
            icon = IdeaComposeIcons.List
        ) {
            if (toc.isEmpty()) {
                Text(
                    text = "No table of contents",
                    style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp, color = JewelTheme.globalColors.text.info),
                    modifier = Modifier.padding(12.dp)
                )
            } else {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    toc.forEach { item -> IdeaTocItemRow(item, onTocSelected) }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Entities Section
        IdeaCollapsibleSection(
            title = "Entities",
            count = entities.size,
            expanded = entitiesExpanded,
            onToggle = { entitiesExpanded = !entitiesExpanded },
            icon = IdeaComposeIcons.Code
        ) {
            if (entities.isEmpty()) {
                Text(
                    text = "No entities extracted",
                    style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp, color = JewelTheme.globalColors.text.info),
                    modifier = Modifier.padding(12.dp)
                )
            } else {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    entities.forEach { entity -> IdeaEntityItemRow(entity, onEntitySelected) }
                }
            }
        }
    }
}

@Composable
private fun IdeaCollapsibleSection(
    title: String,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(JewelTheme.globalColors.panelBackground.copy(alpha = 0.5f))
    ) {
        Column {
            // Header
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, Modifier.size(16.dp), AutoDevColors.Indigo.c400)
                    Text(title, style = JewelTheme.defaultTextStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
                    if (count > 0) {
                        Box(
                            Modifier.clip(RoundedCornerShape(4.dp))
                                .background(AutoDevColors.Indigo.c100.copy(0.5f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(count.toString(), style = JewelTheme.defaultTextStyle.copy(fontSize = 10.sp, color = AutoDevColors.Indigo.c700))
                        }
                    }
                }
                Icon(
                    if (expanded) IdeaComposeIcons.ExpandLess else IdeaComposeIcons.ExpandMore,
                    if (expanded) "Collapse" else "Expand",
                    Modifier.size(16.dp),
                    JewelTheme.globalColors.text.info
                )
            }

            // Content
            if (expanded) {
                Divider(Orientation.Horizontal, modifier = Modifier.fillMaxWidth().height(1.dp))
                Box(Modifier.padding(8.dp)) { content() }
            }
        }
    }
}

@Composable
private fun IdeaTocItemRow(item: TOCItem, onTocSelected: (TOCItem) -> Unit) {
    val safeLevel = item.level.coerceAtLeast(1)
    Column {
        Row(
            Modifier.fillMaxWidth()
                .clickable { onTocSelected(item) }
                .padding(start = ((safeLevel - 1) * 12).dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                item.title,
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 12.sp,
                    fontWeight = if (safeLevel == 1) FontWeight.SemiBold else FontWeight.Normal
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            item.page?.let { page ->
                Text("P$page", style = JewelTheme.defaultTextStyle.copy(fontSize = 10.sp, color = JewelTheme.globalColors.text.info))
            }
        }
        item.children.forEach { child -> IdeaTocItemRow(child, onTocSelected) }
    }
}

@Composable
private fun IdeaEntityItemRow(entity: Entity, onEntitySelected: (Entity) -> Unit) {
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(JewelTheme.globalColors.panelBackground.copy(alpha = 0.3f))
            .clickable { onEntitySelected(entity) }
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val (icon, color) = when (entity) {
                is Entity.Term -> IdeaComposeIcons.Description to AutoDevColors.Green.c400
                is Entity.API -> IdeaComposeIcons.Cloud to AutoDevColors.Blue.c400
                is Entity.ClassEntity -> IdeaComposeIcons.Code to AutoDevColors.Indigo.c400
                is Entity.FunctionEntity -> IdeaComposeIcons.Terminal to AutoDevColors.Amber.c400
                is Entity.ConstructorEntity -> IdeaComposeIcons.Build to AutoDevColors.Cyan.c400
            }
            Icon(icon, null, Modifier.size(16.dp), color)

            Column(Modifier.weight(1f)) {
                Text(
                    entity.name,
                    style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val desc = when (entity) {
                    is Entity.Term -> entity.definition
                    is Entity.API -> entity.signature
                    is Entity.ClassEntity -> "Class"
                    is Entity.FunctionEntity -> entity.signature
                    is Entity.ConstructorEntity -> entity.signature ?: "Constructor"
                }
                if (!desc.isNullOrEmpty()) {
                    Text(
                        desc,
                        style = JewelTheme.defaultTextStyle.copy(fontSize = 10.sp, color = JewelTheme.globalColors.text.info),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

