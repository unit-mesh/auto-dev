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
import cafe.adriel.bonsai.core.node.Leaf
import cafe.adriel.bonsai.core.tree.Tree
import cafe.adriel.bonsai.filesystem.FileSystemBonsaiStyle
import cafe.adriel.bonsai.filesystem.FileSystemTree
import java.io.File

/**
 * JVM implementation of FileSystemTreeView using Bonsai library
 */
@Composable
actual fun FileSystemTreeView(
    rootPath: String,
    onFileClick: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier
) {
    val tree = FileSystemTree(
        rootPath = File(rootPath),
        selfInclude = false
    )
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        // Header
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
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
            modifier = Modifier
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
                style = FileSystemBonsaiStyle().copy(
                    nodeIconSize = 18.dp,
                    nodeNameTextStyle = MaterialTheme.typography.bodyMedium,
                    nodeCollapsedIcon = { node ->
                        val icon = if (node is BranchNode) {
                            Icons.Default.Folder
                        } else {
                            getFileIcon(node.content.name)
                        }
                        rememberVectorPainter(icon)
                    },
                    nodeExpandedIcon = { node ->
                        val icon = if (node is BranchNode) {
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
 * Check if a file is a code/text file that should be opened
 */
private fun isCodeFile(path: String): Boolean {
    val fileName = path.substringAfterLast('/')
    val extension = fileName.substringAfterLast('.', "").lowercase()
    
    // 二进制文件扩展名 - 不应该打开
    val binaryExtensions = setOf(
        // 编译产物
        "class", "jar", "war", "ear", "zip", "tar", "gz", "bz2", "7z", "rar",
        // 可执行文件
        "exe", "dll", "so", "dylib", "bin", "app",
        // 图片
        "png", "jpg", "jpeg", "gif", "bmp", "ico", "svg", "webp", "tiff",
        // 视频
        "mp4", "avi", "mov", "mkv", "wmv", "flv", "webm",
        // 音频
        "mp3", "wav", "ogg", "flac", "aac", "wma",
        // 字体
        "ttf", "otf", "woff", "woff2", "eot",
        // 数据库
        "db", "sqlite", "sqlite3",
        // 其他
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx"
    )
    
    // 检查是否是二进制文件
    if (extension in binaryExtensions) {
        return false
    }
    
    // 可以打开的代码/文本文件扩展名
    val codeExtensions = setOf(
        "kt", "kts", "java", "js", "ts", "tsx", "jsx", "py", "go", "rs",
        "c", "cpp", "h", "hpp", "cc", "cxx", "cs", "swift", "rb", "php",
        "html", "htm", "css", "scss", "sass", "less", "json", "xml", "yaml", "yml",
        "md", "markdown", "txt", "sh", "bash", "zsh", "fish", "sql", 
        "gradle", "properties", "toml", "ini", "conf", "config",
        "proto", "graphql", "vue", "svelte", "astro"
    )
    
    if (extension in codeExtensions) {
        return true
    }
    
    // 无扩展名文件 - 检查文件名
    if (extension.isEmpty() || extension == fileName.lowercase()) {
        val noExtensionFiles = setOf(
            "dockerfile", "makefile", "rakefile", "gemfile", "vagrantfile",
            "jenkinsfile", "podfile", "cartfile", "brewfile",
            "gradlew", "gradlew.bat", "mvnw", "mvnw.cmd",
            ".gitignore", ".dockerignore", ".npmignore", ".editorconfig",
            ".eslintrc", ".prettierrc", ".babelrc", ".travis.yml",
            "cmakelists.txt", "license", "readme", "changelog", "contributing"
        )
        return fileName.lowercase() in noExtensionFiles
    }
    
    return false
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
