package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import cafe.adriel.bonsai.core.Bonsai
import cafe.adriel.bonsai.core.node.BranchNode
import cafe.adriel.bonsai.filesystem.FileSystemBonsaiStyle
import cafe.adriel.bonsai.filesystem.FileSystemTree
import java.io.File

/**
 * Android implementation of FileSystemTreeView using Bonsai library
 */
@Composable
actual fun FileSystemTreeView(
    rootPath: String,
    onFileClick: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier
) {
    val tree =
        FileSystemTree(
            rootPath = File(rootPath),
            selfInclude = false
        )

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        // Header
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Folder",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "PROJECT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider()

        // Tree view with custom styling
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(8.dp)
        ) {
            Bonsai(
                tree = tree,
                onClick = { node ->
                    // Only handle file clicks - Bonsai automatically handles folder expansion
                    if (node !is BranchNode) {
                        val file = node.content
                        val filePath = file.toString()
                        if (isCodeFile(filePath)) {
                            onFileClick(filePath)
                        }
                    }
                },
                style =
                    FileSystemBonsaiStyle().copy(
                        nodeIconSize = 18.dp,
                        nodeNameTextStyle = MaterialTheme.typography.bodyMedium,
                        nodeCollapsedIcon = { node ->
                            val icon =
                                if (node is BranchNode) {
                                    Icons.Default.Folder
                                } else {
                                    getFileIcon(node.content.name)
                                }
                            rememberVectorPainter(icon)
                        },
                        nodeExpandedIcon = { node ->
                            val icon =
                                if (node is BranchNode) {
                                    Icons.Default.FolderOpen
                                } else {
                                    getFileIcon(node.content.name)
                                }
                            rememberVectorPainter(icon)
                        }
                    )
            )
        }
    }
}

/**
 * Check if a file is a code file
 */
private fun isCodeFile(path: String): Boolean {
    val extension = path.substringAfterLast('.', "")
    val codeExtensions =
        setOf(
            "kt", "java", "js", "ts", "tsx", "jsx", "py", "go", "rs",
            "c", "cpp", "h", "hpp", "cs", "swift", "rb", "php",
            "html", "css", "scss", "sass", "json", "xml", "yaml", "yml",
            "md", "txt", "sh", "bash", "sql", "gradle", "properties", "kts"
        )
    return extension.lowercase() in codeExtensions
}

/**
 * Get appropriate icon for file type
 */
private fun getFileIcon(fileName: String): androidx.compose.ui.graphics.vector.ImageVector {
    val extension = fileName.substringAfterLast('.', "").lowercase()
    return when (extension) {
        "kt", "kts", "java", "js", "ts", "py", "go", "rs", "c", "cpp", "cs" -> Icons.Default.Code
        "md", "txt" -> Icons.Default.Article
        "json", "xml", "yaml", "yml" -> Icons.Default.DataObject
        "html", "css" -> Icons.Default.Web
        else -> Icons.Default.Description
    }
}
