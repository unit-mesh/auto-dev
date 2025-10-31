package cc.unitmesh.devins.ui.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.filesystem.ProjectFileSystem

data class FileTreeNode(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val isExpanded: Boolean = false,
    val level: Int = 0
)

@Composable
fun DevInsFileTree(
    projectRoot: String?,
    fileSystem: ProjectFileSystem?,
    onFileSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedDirs by remember { mutableStateOf(setOf<String>()) }
    
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // 标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📁 Project Explorer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Divider()

            // 文件树内容
            if (projectRoot != null && fileSystem != null && fileSystem.exists(projectRoot)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    val nodes = buildFileTree(projectRoot, fileSystem, expandedDirs)
                    items(nodes) { node ->
                        FileTreeItem(
                            node = node,
                            onFileClick = { path, isDir ->
                                if (isDir) {
                                    expandedDirs = if (path in expandedDirs) {
                                        expandedDirs - path
                                    } else {
                                        expandedDirs + path
                                    }
                                } else if (isDevInsFile(path)) {
                                    onFileSelected(path)
                                }
                            }
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No project opened",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FileTreeItem(
    node: FileTreeNode,
    onFileClick: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onFileClick(node.path, node.isDirectory) }
            .padding(
                start = (node.level * 16).dp,
                top = 4.dp,
                bottom = 4.dp,
                end = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = getFileIcon(node.path, node.isDirectory, node.isExpanded),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = getFileIconColor(node.path, node.isDirectory)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = node.name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDevInsFile(node.path)) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

private fun buildFileTree(
    rootPath: String,
    fileSystem: ProjectFileSystem,
    expandedDirs: Set<String>,
    level: Int = 0
): List<FileTreeNode> {
    val result = mutableListOf<FileTreeNode>()
    val rootName = rootPath.substringAfterLast('/')

    if (level == 0) {
        result.add(FileTreeNode(
            path = rootPath,
            name = rootName,
            isDirectory = true,
            isExpanded = rootPath in expandedDirs,
            level = level
        ))
    }

    if (fileSystem.isDirectory(rootPath) && (level == 0 || rootPath in expandedDirs)) {
        try {
            val files = fileSystem.listFiles(rootPath)
                .filter { shouldIncludeFile(it, fileSystem) }
                .sortedWith(compareBy(
                    { !fileSystem.isDirectory(it) },
                    { it.substringAfterLast('/').lowercase() }
                ))

            files.forEach { filePath ->
                val fileName = filePath.substringAfterLast('/')
                val isDir = fileSystem.isDirectory(filePath)
                val isExpanded = filePath in expandedDirs

                result.add(FileTreeNode(
                    path = filePath,
                    name = fileName,
                    isDirectory = isDir,
                    isExpanded = isExpanded,
                    level = level + 1
                ))

                if (isDir && isExpanded) {
                    result.addAll(buildFileTree(filePath, fileSystem, expandedDirs, level + 1))
                }
            }
        } catch (e: Exception) {
            // Handle permission errors gracefully
        }
    }

    return result
}

private fun getFileIcon(path: String, isDirectory: Boolean, isExpanded: Boolean): ImageVector {
    return when {
        isDirectory -> if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight
        isDevInsFile(path) -> Icons.Default.Description
        getExtension(path) in setOf("kt", "java") -> Icons.Default.Code
        getExtension(path) in setOf("js", "ts") -> Icons.Default.Javascript
        getExtension(path) in setOf("py") -> Icons.Default.Code
        getExtension(path) in setOf("json", "yaml", "yml") -> Icons.Default.Settings
        getExtension(path) in setOf("md") -> Icons.Default.Article
        else -> Icons.Default.InsertDriveFile
    }
}

@Composable
private fun getFileIconColor(path: String, isDirectory: Boolean): androidx.compose.ui.graphics.Color {
    return when {
        isDirectory -> MaterialTheme.colorScheme.primary
        isDevInsFile(path) -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private fun isDevInsFile(path: String): Boolean {
    val extension = getExtension(path)
    return extension in setOf("devin", "devins")
}

private fun getExtension(path: String): String {
    return path.substringAfterLast('.', "").lowercase()
}

private fun shouldIncludeFile(path: String, fileSystem: ProjectFileSystem): Boolean {
    val fileName = path.substringAfterLast('/')

    if (fileSystem.isDirectory(path)) {
        return shouldIncludeDirectory(fileName)
    }

    val extension = getExtension(path)
    return extension in setOf(
        "devin", "devins", "kt", "java", "js", "ts", "py",
        "json", "yaml", "yml", "md", "txt", "xml", "html", "css"
    )
}

private fun shouldIncludeDirectory(name: String): Boolean {
    val lowerName = name.lowercase()
    return !lowerName.startsWith(".") &&
           lowerName !in setOf("node_modules", "build", "target", ".gradle", ".idea", "__pycache__")
}
