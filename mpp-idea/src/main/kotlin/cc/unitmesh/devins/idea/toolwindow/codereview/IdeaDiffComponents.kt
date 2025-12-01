package cc.unitmesh.devins.idea.toolwindow.codereview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.agent.diff.ChangeType
import cc.unitmesh.agent.diff.DiffHunk
import cc.unitmesh.agent.diff.DiffLine
import cc.unitmesh.agent.diff.DiffLineType
import cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons
import cc.unitmesh.devins.ui.compose.agent.codereview.CommitInfo
import cc.unitmesh.devins.ui.compose.agent.codereview.DiffFileInfo
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.*

internal enum class IdeaFileViewMode { LIST, TREE }

@Composable
internal fun DiffViewerPanel(
    diffFiles: List<DiffFileInfo>,
    selectedCommits: List<CommitInfo>,
    selectedCommitIndices: Set<Int>,
    isLoadingDiff: Boolean,
    onViewFile: ((String) -> Unit)? = null,
    onRefreshIssue: ((Int) -> Unit)? = null,
    onConfigureToken: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf(IdeaFileViewMode.LIST) }
    Column(modifier = modifier.fillMaxSize().background(JewelTheme.globalColors.panelBackground).padding(8.dp)) {
        if (selectedCommits.isNotEmpty()) {
            IdeaCommitInfoCard(selectedCommits, selectedCommitIndices.toList(), onRefreshIssue, onConfigureToken)
            Spacer(modifier = Modifier.height(8.dp))
        }
        DiffFilesHeader(diffFiles.size, viewMode) { viewMode = it }
        Divider(Orientation.Horizontal, modifier = Modifier.fillMaxWidth().height(1.dp))
        DiffContentArea(diffFiles, selectedCommits, isLoadingDiff, viewMode, onViewFile)
    }
}

@Composable
private fun DiffFilesHeader(fileCount: Int, viewMode: IdeaFileViewMode, onViewModeChange: (IdeaFileViewMode) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text = "Files changed ($fileCount)", style = JewelTheme.defaultTextStyle.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = { onViewModeChange(IdeaFileViewMode.LIST) }, modifier = Modifier.size(28.dp)) {
                Icon(IdeaComposeIcons.List, "List view", tint = if (viewMode == IdeaFileViewMode.LIST) AutoDevColors.Indigo.c600 else JewelTheme.globalColors.text.info, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = { onViewModeChange(IdeaFileViewMode.TREE) }, modifier = Modifier.size(28.dp)) {
                Icon(IdeaComposeIcons.AccountTree, "Tree view", tint = if (viewMode == IdeaFileViewMode.TREE) AutoDevColors.Indigo.c600 else JewelTheme.globalColors.text.info, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun DiffContentArea(diffFiles: List<DiffFileInfo>, selectedCommits: List<CommitInfo>, isLoadingDiff: Boolean, viewMode: IdeaFileViewMode, onViewFile: ((String) -> Unit)?) {
    when {
        isLoadingDiff -> Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CircularProgressIndicator()
                Text("Loading diff...", style = JewelTheme.defaultTextStyle.copy(color = JewelTheme.globalColors.text.info))
            }
        }
        diffFiles.isEmpty() -> Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(if (selectedCommits.isEmpty()) "Select a commit to view diff" else "No file changes in this commit", style = JewelTheme.defaultTextStyle.copy(color = JewelTheme.globalColors.text.info))
        }
        else -> when (viewMode) {
            IdeaFileViewMode.LIST -> IdeaCompactFileListView(diffFiles, onViewFile)
            IdeaFileViewMode.TREE -> IdeaFileTreeView(diffFiles, onViewFile)
        }
    }
}

@Composable
internal fun IdeaCompactFileListView(files: List<DiffFileInfo>, onViewFile: ((String) -> Unit)?) {
    var expandedFileIndex by remember { mutableStateOf<Int?>(null) }
    LazyColumn(state = rememberLazyListState(), modifier = Modifier.fillMaxSize()) {
        itemsIndexed(files) { index, file ->
            IdeaCompactFileDiffItem(file, expandedFileIndex == index, { expandedFileIndex = if (expandedFileIndex == index) null else index }, onViewFile)
        }
    }
}

@Composable
private fun IdeaCompactFileDiffItem(file: DiffFileInfo, isExpanded: Boolean, onToggleExpand: () -> Unit, onViewFile: ((String) -> Unit)?) {
    val changeColor = when (file.changeType) { ChangeType.CREATE -> AutoDevColors.Green.c400; ChangeType.DELETE -> AutoDevColors.Red.c400; ChangeType.RENAME -> AutoDevColors.Amber.c400; else -> AutoDevColors.Blue.c400 }
    val changeIcon = when (file.changeType) { ChangeType.CREATE -> IdeaComposeIcons.Add; ChangeType.DELETE -> IdeaComposeIcons.Delete; ChangeType.RENAME -> IdeaComposeIcons.DriveFileRenameOutline; else -> IdeaComposeIcons.Edit }
    Column(modifier = Modifier.fillMaxWidth()) {
        FileDiffItemHeader(file, isExpanded, changeColor, changeIcon, onToggleExpand, onViewFile)
        AnimatedVisibility(visible = isExpanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, bottom = 8.dp).background(JewelTheme.globalColors.panelBackground.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(8.dp)) {
                file.hunks.forEachIndexed { i, hunk -> if (i > 0) Spacer(Modifier.height(8.dp)); IdeaDiffHunkView(hunk) }
            }
        }
        Divider(Orientation.Horizontal, modifier = Modifier.fillMaxWidth().height(1.dp))
    }
}

@Composable
private fun FileDiffItemHeader(file: DiffFileInfo, isExpanded: Boolean, changeColor: Color, changeIcon: androidx.compose.ui.graphics.vector.ImageVector, onToggleExpand: () -> Unit, onViewFile: ((String) -> Unit)?) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onToggleExpand() }.padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(if (isExpanded) IdeaComposeIcons.ExpandMore else IdeaComposeIcons.ChevronRight, if (isExpanded) "Collapse" else "Expand", tint = JewelTheme.globalColors.text.info, modifier = Modifier.size(16.dp))
            Icon(changeIcon, file.changeType.name, tint = changeColor, modifier = Modifier.size(14.dp))
            Text(file.path.split("/").lastOrNull() ?: file.path, style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium))
            val dir = file.path.substringBeforeLast("/", "")
            if (dir.isNotEmpty()) Text(dir, style = JewelTheme.defaultTextStyle.copy(fontSize = 11.sp, color = JewelTheme.globalColors.text.info.copy(alpha = 0.6f)))
        }
        FileLineCountBadges(file, onViewFile)
    }
}

@Composable
private fun FileLineCountBadges(file: DiffFileInfo, onViewFile: ((String) -> Unit)?) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        onViewFile?.let { IconButton(onClick = { it(file.path) }, modifier = Modifier.size(24.dp)) { Icon(IdeaComposeIcons.Visibility, "View file", tint = JewelTheme.globalColors.text.info, modifier = Modifier.size(14.dp)) } }
        val added = file.hunks.sumOf { h -> h.lines.count { it.type == DiffLineType.ADDED } }
        val deleted = file.hunks.sumOf { h -> h.lines.count { it.type == DiffLineType.DELETED } }
        if (added > 0) Text("+$added", style = JewelTheme.defaultTextStyle.copy(fontSize = 10.sp, color = AutoDevColors.Green.c400, fontWeight = FontWeight.Bold))
        if (deleted > 0) Text("-$deleted", style = JewelTheme.defaultTextStyle.copy(fontSize = 10.sp, color = AutoDevColors.Red.c400, fontWeight = FontWeight.Bold))
    }
}

@Composable
internal fun IdeaDiffHunkView(hunk: DiffHunk) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(hunk.header, style = JewelTheme.defaultTextStyle.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = AutoDevColors.Indigo.c400))
        Spacer(modifier = Modifier.height(4.dp))
        hunk.lines.forEach { line -> if (line.type != DiffLineType.HEADER) IdeaDiffLineView(line) }
    }
}

@Composable
private fun IdeaDiffLineView(line: DiffLine) {
    val (bgColor, textColor, prefix) = when (line.type) {
        DiffLineType.ADDED -> Triple(AutoDevColors.Green.c400.copy(alpha = 0.15f), AutoDevColors.Green.c400, "+")
        DiffLineType.DELETED -> Triple(AutoDevColors.Red.c400.copy(alpha = 0.15f), AutoDevColors.Red.c400, "-")
        DiffLineType.CONTEXT -> Triple(Color.Transparent, JewelTheme.globalColors.text.normal, " ")
        DiffLineType.HEADER -> return
    }
    Row(modifier = Modifier.fillMaxWidth().background(bgColor).padding(horizontal = 4.dp, vertical = 1.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(line.oldLineNumber?.toString()?.padStart(4) ?: "    ", style = JewelTheme.defaultTextStyle.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = JewelTheme.globalColors.text.info.copy(alpha = 0.5f)))
        Text(line.newLineNumber?.toString()?.padStart(4) ?: "    ", style = JewelTheme.defaultTextStyle.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = JewelTheme.globalColors.text.info.copy(alpha = 0.5f)))
        Text(prefix, style = JewelTheme.defaultTextStyle.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = textColor, fontWeight = FontWeight.Bold))
        Text(line.content, style = JewelTheme.defaultTextStyle.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = textColor))
    }
}

internal sealed class FileTreeNode {
    data class Directory(val name: String, val path: String, val files: List<DiffFileInfo>) : FileTreeNode()
    data class File(val file: DiffFileInfo) : FileTreeNode()
}

private fun buildFileTreeStructure(files: List<DiffFileInfo>): List<FileTreeNode> {
    val result = mutableListOf<FileTreeNode>()
    val directoryMap = mutableMapOf<String, MutableList<DiffFileInfo>>()
    files.forEach { file ->
        val directory = file.path.substringBeforeLast("/", "")
        if (directory.isEmpty()) result.add(FileTreeNode.File(file))
        else directoryMap.getOrPut(directory) { mutableListOf() }.add(file)
    }
    directoryMap.entries.sortedBy { it.key }.forEach { (path, dirFiles) ->
        result.add(FileTreeNode.Directory(path.split("/").lastOrNull() ?: path, path, dirFiles.sortedBy { it.path }))
    }
    return result
}

@Composable
internal fun IdeaFileTreeView(files: List<DiffFileInfo>, onViewFile: ((String) -> Unit)?) {
    val treeNodes = remember(files) { buildFileTreeStructure(files) }
    var expandedDirs by remember { mutableStateOf(setOf<String>()) }
    var expandedFilePath by remember { mutableStateOf<String?>(null) }

    LazyColumn(state = rememberLazyListState(), modifier = Modifier.fillMaxSize()) {
        treeNodes.forEach { node ->
            when (node) {
                is FileTreeNode.Directory -> {
                    item(key = "dir_${node.path}") {
                        IdeaDirectoryTreeItem(node, expandedDirs.contains(node.path)) {
                            expandedDirs = if (expandedDirs.contains(node.path)) expandedDirs - node.path else expandedDirs + node.path
                        }
                    }
                    if (expandedDirs.contains(node.path)) {
                        node.files.forEachIndexed { index, file ->
                            item(key = "file_${node.path}_$index") {
                                IdeaFileTreeItemCompact(file, expandedFilePath == file.path, { expandedFilePath = if (expandedFilePath == file.path) null else file.path }, onViewFile, 1)
                            }
                        }
                    }
                }
                is FileTreeNode.File -> {
                    item(key = "file_root_${node.file.path}") {
                        IdeaFileTreeItemCompact(node.file, expandedFilePath == node.file.path, { expandedFilePath = if (expandedFilePath == node.file.path) null else node.file.path }, onViewFile, 0)
                    }
                }
            }
        }
    }
}

@Composable
private fun IdeaDirectoryTreeItem(directory: FileTreeNode.Directory, isExpanded: Boolean, onToggle: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (isExpanded) IdeaComposeIcons.ExpandMore else IdeaComposeIcons.ChevronRight, if (isExpanded) "Collapse" else "Expand", tint = JewelTheme.globalColors.text.info, modifier = Modifier.size(16.dp))
        Icon(if (isExpanded) IdeaComposeIcons.FolderOpen else IdeaComposeIcons.Folder, "Directory", tint = AutoDevColors.Amber.c400, modifier = Modifier.size(16.dp))
        Text(directory.name, style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium))
        Text("(${directory.files.size})", style = JewelTheme.defaultTextStyle.copy(fontSize = 11.sp, color = JewelTheme.globalColors.text.info.copy(alpha = 0.6f)))
    }
}

@Composable
private fun IdeaFileTreeItemCompact(file: DiffFileInfo, isExpanded: Boolean, onToggleExpand: () -> Unit, onViewFile: ((String) -> Unit)?, indentLevel: Int) {
    val changeColor = when (file.changeType) { ChangeType.CREATE -> AutoDevColors.Green.c400; ChangeType.DELETE -> AutoDevColors.Red.c400; ChangeType.RENAME -> AutoDevColors.Amber.c400; else -> AutoDevColors.Blue.c400 }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().clickable { onToggleExpand() }.padding(start = (8 + indentLevel * 16).dp, end = 8.dp, top = 4.dp, bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(if (isExpanded) IdeaComposeIcons.ExpandMore else IdeaComposeIcons.ChevronRight, if (isExpanded) "Collapse" else "Expand", tint = JewelTheme.globalColors.text.info, modifier = Modifier.size(14.dp))
                Icon(IdeaComposeIcons.Description, "File", tint = changeColor, modifier = Modifier.size(14.dp))
                Text(file.path.split("/").lastOrNull() ?: file.path, style = JewelTheme.defaultTextStyle.copy(fontSize = 11.sp))
            }
            onViewFile?.let { IconButton(onClick = { it(file.path) }, modifier = Modifier.size(20.dp)) { Icon(IdeaComposeIcons.Visibility, "View file", tint = JewelTheme.globalColors.text.info, modifier = Modifier.size(12.dp)) } }
        }
        AnimatedVisibility(visible = isExpanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(modifier = Modifier.fillMaxWidth().padding(start = (24 + indentLevel * 16).dp, end = 8.dp, bottom = 8.dp).background(JewelTheme.globalColors.panelBackground.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(8.dp)) {
                file.hunks.forEachIndexed { i, hunk -> if (i > 0) Spacer(Modifier.height(8.dp)); IdeaDiffHunkView(hunk) }
            }
        }
    }
}
