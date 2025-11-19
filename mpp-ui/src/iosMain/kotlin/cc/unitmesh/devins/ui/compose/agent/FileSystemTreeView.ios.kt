package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import platform.Foundation.NSError
import platform.Foundation.NSFileManager

/**
 * iOS implementation of FileSystemTreeView
 * Uses a simplified tree view
 */
@Composable
actual fun FileSystemTreeView(
    rootPath: String,
    onFileClick: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier
) {
    var files by remember(rootPath) { mutableStateOf<List<FileItem>>(emptyList()) }
    val fileManager = remember { NSFileManager.defaultManager }

    LaunchedEffect(rootPath) {
        files = listFiles(rootPath, fileManager)
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Files",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        }

        // File List
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
        ) {
            items(files) { file ->
                FileTreeItem(
                    file = file,
                    level = 0,
                    onFileClick = onFileClick,
                    fileManager = fileManager
                )
            }
        }
    }
}

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val children: List<FileItem> = emptyList()
)

@OptIn(ExperimentalForeignApi::class)
private fun listFiles(path: String, fileManager: NSFileManager): List<FileItem> {
    val contents = fileManager.contentsOfDirectoryAtPath(path, null) as? List<String> ?: emptyList()
    
    return contents.map { name ->
        val fullPath = "$path/$name"
        var isDir: Boolean = false
        val exists = fileManager.fileExistsAtPath(fullPath, null)
        
        // Try to determine if it's a directory by checking if we can list its contents
        val isDirectory = try {
            fileManager.contentsOfDirectoryAtPath(fullPath, null) != null
        } catch (e: Exception) {
            false
        }
        
        FileItem(
            name = name,
            path = fullPath,
            isDirectory = isDirectory,
            children = emptyList()
        )
    }.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
}

@Composable
fun FileTreeItem(
    file: FileItem,
    level: Int,
    onFileClick: (String) -> Unit,
    fileManager: NSFileManager
) {
    var isExpanded by remember { mutableStateOf(false) }
    var children by remember { mutableStateOf<List<FileItem>>(emptyList()) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (file.isDirectory) {
                        isExpanded = !isExpanded
                        if (isExpanded && children.isEmpty()) {
                            children = listFiles(file.path, fileManager)
                        }
                    } else {
                        onFileClick(file.path)
                    }
                }
                .padding(vertical = 4.dp)
                .padding(start = (level * 16).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                contentDescription = null,
                tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
        }

        if (isExpanded) {
            children.forEach { child ->
                FileTreeItem(
                    file = child,
                    level = level + 1,
                    onFileClick = onFileClick,
                    fileManager = fileManager
                )
            }
        }
    }
}

