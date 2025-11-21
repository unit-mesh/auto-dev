package cc.unitmesh.devins.ui.compose.agent.codereview.diff

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.agent.diff.ChangeType
import cc.unitmesh.devins.ui.compose.agent.codereview.DiffFileInfo
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors

/**
 * Tree view for file changes - groups files by directory
 */
@Composable
fun FileTreeView(
    files: List<DiffFileInfo>,
    onViewFile: ((String) -> Unit)?,
    workspaceRoot: String?
) {
    val treeNodes = remember(files) { buildFileTreeStructure(files) }

    LazyColumn(
        modifier = Modifier.Companion.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(treeNodes) { node ->
            FileTreeNodeItem(
                node = node,
                level = 0,
                onViewFile = if (onViewFile != null && workspaceRoot != null) {
                    { path ->
                        val fullPath = if (path.startsWith("/")) path else "$workspaceRoot/$path"
                        onViewFile(fullPath)
                    }
                } else null
            )
        }
    }
}

fun buildFileTree(files: List<DiffFileInfo>): List<FileTreeNode> {
    val root = mutableMapOf<String, FileTreeNode.Directory>()

    files.forEach { fileInfo ->
        val segments = fileInfo.path.split("/")
        var currentMap = root
        var currentPath = ""

        // Build directory structure
        for (i in 0 until segments.size - 1) {
            val segment = segments[i]
            currentPath = if (currentPath.isEmpty()) segment else "$currentPath/$segment"

            if (!currentMap.containsKey(segment)) {
                val dir = FileTreeNode.Directory(segment, currentPath)
                currentMap[segment] = dir
            }

            val dir = currentMap[segment] as FileTreeNode.Directory
            currentMap = dir.children.associateBy { it.name }.toMutableMap() as MutableMap<String, FileTreeNode.Directory>
        }

        // Add file
        val fileName = segments.last()
        val fileNode = FileTreeNode.File(fileName, fileInfo.path, fileInfo)
        currentMap[fileName] = FileTreeNode.Directory(fileName, fileInfo.path) // dummy

        // Add to parent directory
        if (segments.size > 1) {
            val parentPath = segments.dropLast(1).joinToString("/")
            findDirectory(root.values.toList(), parentPath)?.children?.add(fileNode)
        } else {
            root[fileName] = FileTreeNode.Directory(fileName, fileInfo.path)
        }
    }

    return root.values.toList()
}

/**
 * Build tree structure from flat file list
 */

fun findDirectory(nodes: List<FileTreeNode>, path: String): FileTreeNode.Directory? {
    nodes.forEach { node ->
        if (node is FileTreeNode.Directory) {
            if (node.path == path) return node
            findDirectory(node.children, path)?.let { return it }
        }
    }
    return null
}


/**
 * Truncate a file path by showing first/last segments with ... in middle
 * Examples:
 * - "src/main/kotlin/com/example/project/VeryLongFileName.kt" -> "src/.../VeryLongFileName.kt"
 * - "short/path.kt" -> "short/path.kt"
 */
fun truncateFilePath(path: String): String {
    val segments = path.split("/")

    // If path is short enough, return as is
    if (segments.size <= 2) return path

    // Always use format: first/.../ last
    val first = segments.first()
    val last = segments.last()
    return "$first/.../$last"
}


/**
 * View mode for file changes display
 */
enum class FileViewMode {
    LIST,  // Flat list of files
    TREE   // Tree structure grouped by directory
}

/**
 * Tree node for file display
 */
sealed class FileTreeNode(open val name: String, open val path: String) {
    data class Directory(
        override val name: String,
        override val path: String,
        val children: MutableList<FileTreeNode> = mutableListOf()
    ) : FileTreeNode(name, path)

    data class File(
        override val name: String,
        override val path: String,
        val fileInfo: DiffFileInfo
    ) : FileTreeNode(name, path)
}


/**
 * Build a simpler tree structure that groups files by directories
 */
fun buildFileTreeStructure(files: List<DiffFileInfo>): List<FileTreeNode> {
    val rootDirectories = mutableMapOf<String, FileTreeNode.Directory>()
    val rootFiles = mutableListOf<FileTreeNode.File>()

    files.forEach { fileInfo ->
        val segments = fileInfo.path.split("/")

        if (segments.size == 1) {
            // File in root - create File node, not Directory!
            val fileNode = FileTreeNode.File(
                name = segments[0],
                path = fileInfo.path,
                fileInfo = fileInfo
            )
            rootFiles.add(fileNode)
        } else {
            // File in subdirectory - build directory tree
            var currentPath = ""
            var currentDir: FileTreeNode.Directory? = null

            // Navigate/create directory structure
            for (i in 0 until segments.size - 1) {
                val segment = segments[i]
                currentPath = if (currentPath.isEmpty()) segment else "$currentPath/$segment"

                if (i == 0) {
                    // First level - check root directories
                    if (!rootDirectories.containsKey(segment)) {
                        rootDirectories[segment] = FileTreeNode.Directory(segment, currentPath)
                    }
                    currentDir = rootDirectories[segment]
                } else {
                    // Nested levels - check current directory's children
                    val existingDir = currentDir!!.children
                        .filterIsInstance<FileTreeNode.Directory>()
                        .find { it.name == segment }

                    if (existingDir == null) {
                        val newDir = FileTreeNode.Directory(segment, currentPath)
                        currentDir.children.add(newDir)
                        currentDir = newDir
                    } else {
                        currentDir = existingDir
                    }
                }
            }

            // Add file to its parent directory
            val fileName = segments.last()
            val fileNode = FileTreeNode.File(fileName, fileInfo.path, fileInfo)
            currentDir?.children?.add(fileNode)
        }
    }

    // Combine directories and files, directories first, then sorted by name
    val allNodes = mutableListOf<FileTreeNode>()
    allNodes.addAll(rootDirectories.values.sortedBy { it.name })
    allNodes.addAll(rootFiles.sortedBy { it.name })

    return allNodes
}

fun findDirectoryByPath(
    nodes: Map<String, FileTreeNode.Directory>,
    targetPath: String
): FileTreeNode.Directory? {
    if (targetPath.isEmpty()) return null

    val segments = targetPath.split("/")
    var current = nodes[segments[0]] ?: return null

    for (i in 1 until segments.size) {
        val segment = segments[i]
        current = current.children
            .filterIsInstance<FileTreeNode.Directory>()
            .find { it.name == segment } ?: return null
    }

    return current
}

/**
 * Recursive tree node item renderer
 */
@Composable
fun FileTreeNodeItem(
    node: FileTreeNode,
    level: Int,
    onViewFile: ((String) -> Unit)?
) {
    when (node) {
        is FileTreeNode.Directory -> {
            DirectoryTreeItem(
                directory = node,
                level = level,
                onViewFile = onViewFile
            )
        }
        is FileTreeNode.File -> {
            FileTreeItemCompact(
                file = node.fileInfo,
                level = level,
                onViewFile = onViewFile
            )
        }
    }
}

/**
 * Directory item in tree (expandable)
 */
@Composable
fun DirectoryTreeItem(
    directory: FileTreeNode.Directory,
    level: Int,
    onViewFile: ((String) -> Unit)?
) {
    var isExpanded by remember { mutableStateOf(true) } // Default expanded
    val fileCount = countFiles(directory)

    Column(modifier = Modifier.Companion.fillMaxWidth()) {
        Row(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(
                    start = (level * 16 + 8).dp,
                    top = 4.dp,
                    bottom = 4.dp,
                    end = 8.dp
                ),
            verticalAlignment = Alignment.Companion.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (isExpanded)
                    AutoDevComposeIcons.ExpandMore
                else
                    AutoDevComposeIcons.ChevronRight,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.Companion.size(16.dp)
            )

            Icon(
                imageVector = if (isExpanded)
                    AutoDevComposeIcons.FolderOpen
                else
                    AutoDevComposeIcons.Folder,
                contentDescription = "Directory",
                tint = AutoDevColors.Amber.c600,
                modifier = Modifier.Companion.size(16.dp)
            )

            Text(
                text = directory.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Companion.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp
            )

            Text(
                text = "($fileCount)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontSize = 10.sp
            )
        }

        if (isExpanded) {
            directory.children
                .sortedWith(compareBy({ it !is FileTreeNode.Directory }, { it.name }))
                .forEach { child ->
                    FileTreeNodeItem(
                        node = child,
                        level = level + 1,
                        onViewFile = onViewFile
                    )
                }
        }
    }
}

/**
 * Compact file item in tree
 */
@Composable
fun FileTreeItemCompact(
    file: DiffFileInfo,
    level: Int,
    onViewFile: ((String) -> Unit)?
) {
    var expanded by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier.Companion
            .fillMaxWidth()
            .background(
                if (expanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                else Color.Companion.Transparent
            )
    ) {
        Row(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(
                    start = (level * 16 + 24).dp,
                    top = 4.dp,
                    bottom = 4.dp,
                    end = 8.dp
                ),
            verticalAlignment = Alignment.Companion.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.Companion.weight(1f),
                verticalAlignment = Alignment.Companion.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Change type icon
                Icon(
                    imageVector = when (file.changeType) {
                        ChangeType.CREATE -> AutoDevComposeIcons.Add
                        ChangeType.DELETE -> AutoDevComposeIcons.Delete
                        ChangeType.EDIT -> AutoDevComposeIcons.Edit
                        ChangeType.RENAME -> AutoDevComposeIcons.DriveFileRenameOutline
                    },
                    contentDescription = file.changeType.name,
                    tint = when (file.changeType) {
                        ChangeType.CREATE -> AutoDevColors.Green.c600
                        ChangeType.DELETE -> AutoDevColors.Red.c600
                        ChangeType.EDIT -> AutoDevColors.Blue.c600
                        ChangeType.RENAME -> AutoDevColors.Amber.c600
                    },
                    modifier = Modifier.Companion.size(14.dp)
                )

                // Just show file name in tree (not full path)
                val fileName = file.path.split("/").last()
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Companion.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Companion.Ellipsis,
                    fontSize = 12.sp,
                    modifier = Modifier.Companion.weight(1f, fill = false)
                )

                // Language badge
                file.language?.let { lang ->
                    Text(
                        text = lang,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Companion.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(file.path))
                    },
                    modifier = Modifier.Companion.size(26.dp)
                ) {
                    Icon(
                        imageVector = AutoDevComposeIcons.ContentCopy,
                        contentDescription = "Copy path",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.Companion.size(13.dp)
                    )
                }

                if (onViewFile != null) {
                    IconButton(
                        onClick = { onViewFile(file.path) },
                        modifier = Modifier.Companion.size(26.dp)
                    ) {
                        Icon(
                            imageVector = AutoDevComposeIcons.Visibility,
                            contentDescription = "View file",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.Companion.size(13.dp)
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) AutoDevComposeIcons.ExpandLess else AutoDevComposeIcons.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.Companion.size(14.dp)
                )
            }
        }

        // Expanded diff content
        if (expanded && file.hunks.isNotEmpty()) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp
            )

            Column(
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .padding(start = (level * 16 + 48).dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
            ) {
                file.hunks.forEach { hunk ->
                    DiffHunkView(hunk)
                    Spacer(modifier = Modifier.Companion.height(4.dp))
                }
            }
        }
    }
}

/**
 * Count total files in a directory tree
 */
private fun countFiles(directory: FileTreeNode.Directory): Int {
    return directory.children.sumOf { child ->
        when (child) {
            is FileTreeNode.Directory -> countFiles(child)
            is FileTreeNode.File -> 1
        }
    }
}
