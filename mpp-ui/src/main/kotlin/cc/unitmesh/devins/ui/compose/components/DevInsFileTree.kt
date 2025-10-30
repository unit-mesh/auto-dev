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
import java.io.File

data class FileTreeNode(
    val file: File,
    val isExpanded: Boolean = false,
    val level: Int = 0
)

@Composable
fun DevInsFileTree(
    projectRoot: File?,
    onFileSelected: (File) -> Unit,
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
            if (projectRoot != null && projectRoot.exists()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    val nodes = buildFileTree(projectRoot, expandedDirs)
                    items(nodes) { node ->
                        FileTreeItem(
                            node = node,
                            onFileClick = { file ->
                                if (file.isDirectory) {
                                    expandedDirs = if (file.absolutePath in expandedDirs) {
                                        expandedDirs - file.absolutePath
                                    } else {
                                        expandedDirs + file.absolutePath
                                    }
                                } else if (isDevInsFile(file)) {
                                    onFileSelected(file)
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
    onFileClick: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onFileClick(node.file) }
            .padding(
                start = (node.level * 16).dp,
                top = 4.dp,
                bottom = 4.dp,
                end = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = getFileIcon(node.file, node.isExpanded),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = getFileIconColor(node.file)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = node.file.name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDevInsFile(node.file)) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

private fun buildFileTree(
    root: File,
    expandedDirs: Set<String>,
    level: Int = 0
): List<FileTreeNode> {
    val result = mutableListOf<FileTreeNode>()
    
    if (level == 0) {
        result.add(FileTreeNode(root, root.absolutePath in expandedDirs, level))
    }
    
    if (root.isDirectory && (level == 0 || root.absolutePath in expandedDirs)) {
        try {
            val files = root.listFiles()?.filter { !it.isHidden && shouldIncludeFile(it) }
                ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                ?: emptyList()
            
            files.forEach { file ->
                val isExpanded = file.absolutePath in expandedDirs
                result.add(FileTreeNode(file, isExpanded, level + 1))
                
                if (file.isDirectory && isExpanded) {
                    result.addAll(buildFileTree(file, expandedDirs, level + 1))
                }
            }
        } catch (e: Exception) {
            // Handle permission errors gracefully
        }
    }
    
    return result
}

private fun getFileIcon(file: File, isExpanded: Boolean): ImageVector {
    return when {
        file.isDirectory -> if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight
        isDevInsFile(file) -> Icons.Default.Description
        file.extension.lowercase() in setOf("kt", "java") -> Icons.Default.Code
        file.extension.lowercase() in setOf("js", "ts") -> Icons.Default.Javascript
        file.extension.lowercase() in setOf("py") -> Icons.Default.Code
        file.extension.lowercase() in setOf("json", "yaml", "yml") -> Icons.Default.Settings
        file.extension.lowercase() in setOf("md") -> Icons.Default.Article
        else -> Icons.Default.InsertDriveFile
    }
}

@Composable
private fun getFileIconColor(file: File): androidx.compose.ui.graphics.Color {
    return when {
        file.isDirectory -> MaterialTheme.colorScheme.primary
        isDevInsFile(file) -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private fun isDevInsFile(file: File): Boolean {
    val extension = file.extension.lowercase()
    return extension in setOf("devin", "devins")
}

private fun shouldIncludeFile(file: File): Boolean {
    if (file.isDirectory) {
        return shouldIncludeDirectory(file)
    }
    
    val extension = file.extension.lowercase()
    return extension in setOf(
        "devin", "devins", "kt", "java", "js", "ts", "py", 
        "json", "yaml", "yml", "md", "txt", "xml", "html", "css"
    )
}

private fun shouldIncludeDirectory(dir: File): Boolean {
    val name = dir.name.lowercase()
    return !name.startsWith(".") && 
           name !in setOf("node_modules", "build", "target", ".gradle", ".idea", "__pycache__")
}
