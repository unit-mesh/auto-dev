package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rtextarea.RTextScrollPane
import java.io.File

@Composable
fun FileViewerPanel(
    filePath: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textArea by remember { mutableStateOf<RSyntaxTextArea?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var fileName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var fileContent by remember { mutableStateOf<String?>(null) }

    // 异步加载文件内容
    LaunchedEffect(filePath) {
        isLoading = true
        errorMessage = null
        fileContent = null

        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    errorMessage = "File not found: $filePath"
                    fileName = filePath
                    return@withContext
                }

                fileName = file.name

                // 检查是否是二进制文件
                if (isBinaryFile(file)) {
                    errorMessage = "Cannot open binary file: ${file.name}"
                    return@withContext
                }

                // 检查文件大小（限制 10MB）
                val maxSize = 10 * 1024 * 1024 // 10MB
                if (file.length() > maxSize) {
                    errorMessage = "File too large (${file.length() / 1024 / 1024}MB). Maximum size is 10MB."
                    return@withContext
                }

                // 异步读取文件
                try {
                    fileContent = file.readText()
                } catch (e: java.nio.charset.MalformedInputException) {
                    errorMessage = "Cannot decode file (likely binary): ${file.name}"
                }
            } catch (e: Exception) {
                errorMessage = "Error loading file: ${e.message}"
                fileName = filePath
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = filePath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = AutoDevComposeIcons.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Content
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    // Loading state
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading file...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                errorMessage != null -> {
                    // Error state
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "❌ Error",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                fileContent != null -> {
                    // RSyntaxTextArea content
                    SwingPanel(
                        background = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxSize(),
                        factory = {
                            val file = File(filePath)
                            val area =
                                RSyntaxTextArea().apply {
                                    text = fileContent ?: ""
                                    isEditable = false
                                    syntaxEditingStyle = getSyntaxStyleForFile(file)
                                    isCodeFoldingEnabled = true
                                    antiAliasingEnabled = true
                                    tabSize = 4
                                    margin = java.awt.Insets(5, 5, 5, 5)
                                }
                            textArea = area

                            RTextScrollPane(area).apply {
                                isFoldIndicatorEnabled = true
                            }
                        },
                        update = {
                            // Update text if content changes
                            textArea?.let { area ->
                                if (area.text != fileContent) {
                                    val file = File(filePath)
                                    area.text = fileContent ?: ""
                                    area.syntaxEditingStyle = getSyntaxStyleForFile(file)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Check if a file is likely a binary file
 */
private fun isBinaryFile(file: File): Boolean {
    val extension = file.extension.lowercase()

    // 常见的二进制文件扩展名
    val binaryExtensions =
        setOf(
            // 编译产物
            "class", "jar", "war", "ear", "zip", "tar", "gz", "bz2", "7z", "rar",
            // 可执行文件
            "exe", "dll", "so", "dylib", "bin", "app",
            // 图片
            "png", "jpg", "jpeg", "gif", "bmp", "ico", "webp", "tiff",
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

    if (extension in binaryExtensions) {
        return true
    }

    // 对于无扩展名或未知扩展名的文件，检查前 512 字节是否包含二进制字符
    if (file.length() > 0) {
        try {
            val bytesToCheck = minOf(512, file.length().toInt())
            val bytes = file.inputStream().use { it.readNBytes(bytesToCheck) }

            // 检查是否包含 NULL 字节或其他控制字符（除了常见的如 \n, \r, \t）
            val binaryThreshold = 0.3 // 如果超过 30% 是非文本字符，认为是二进制
            var nonTextCount = 0

            for (byte in bytes) {
                val b = byte.toInt() and 0xFF
                // NULL 字节或其他控制字符（除了 \n=10, \r=13, \t=9）
                if (b == 0 || (b < 32 && b != 9 && b != 10 && b != 13)) {
                    nonTextCount++
                }
            }

            return (nonTextCount.toDouble() / bytes.size) > binaryThreshold
        } catch (e: Exception) {
            // 读取失败，保守起见认为是二进制
            return true
        }
    }

    return false
}

private fun getSyntaxStyleForFile(file: File): String {
    val extension = file.extension.lowercase()
    val fileName = file.name.lowercase()

    return when {
        // 基于扩展名
        extension == "java" -> SyntaxConstants.SYNTAX_STYLE_JAVA
        extension in setOf("kt", "kts") -> SyntaxConstants.SYNTAX_STYLE_KOTLIN
        extension == "js" -> SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT
        extension == "ts" || extension == "tsx" -> SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT
        extension == "py" -> SyntaxConstants.SYNTAX_STYLE_PYTHON
        extension == "xml" -> SyntaxConstants.SYNTAX_STYLE_XML
        extension in setOf("html", "htm") -> SyntaxConstants.SYNTAX_STYLE_HTML
        extension == "css" || extension == "scss" || extension == "sass" -> SyntaxConstants.SYNTAX_STYLE_CSS
        extension == "json" -> SyntaxConstants.SYNTAX_STYLE_JSON
        extension in setOf("yaml", "yml") -> SyntaxConstants.SYNTAX_STYLE_YAML
        extension in setOf("md", "markdown") -> SyntaxConstants.SYNTAX_STYLE_MARKDOWN
        extension == "sql" -> SyntaxConstants.SYNTAX_STYLE_SQL
        extension in setOf("sh", "bash", "zsh") -> SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL
        extension in setOf("c", "h") -> SyntaxConstants.SYNTAX_STYLE_C
        extension in setOf("cpp", "hpp", "cc", "cxx") -> SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS
        extension == "go" -> SyntaxConstants.SYNTAX_STYLE_GO
        extension == "rs" -> SyntaxConstants.SYNTAX_STYLE_RUST
        extension == "rb" -> SyntaxConstants.SYNTAX_STYLE_RUBY
        extension == "php" -> SyntaxConstants.SYNTAX_STYLE_PHP
        extension == "cs" -> SyntaxConstants.SYNTAX_STYLE_CSHARP
        extension == "scala" -> SyntaxConstants.SYNTAX_STYLE_SCALA
        extension == "gradle" -> SyntaxConstants.SYNTAX_STYLE_GROOVY
        extension in setOf("properties", "ini", "conf", "toml") -> SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE

        // 基于文件名（无扩展名文件）
        fileName == "dockerfile" -> SyntaxConstants.SYNTAX_STYLE_DOCKERFILE
        fileName == "makefile" || fileName == "rakefile" -> SyntaxConstants.SYNTAX_STYLE_MAKEFILE
        fileName in setOf("gradlew", "gradlew.bat") -> SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL
        fileName.startsWith(".") -> SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE

        else -> SyntaxConstants.SYNTAX_STYLE_NONE
    }
}
