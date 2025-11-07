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
                
                // 检查文件大小（限制 10MB）
                val maxSize = 10 * 1024 * 1024 // 10MB
                if (file.length() > maxSize) {
                    errorMessage = "File too large (${file.length() / 1024 / 1024}MB). Maximum size is 10MB."
                    return@withContext
                }
                
                // 异步读取文件
                fileContent = file.readText()
            } catch (e: Exception) {
                errorMessage = "Error loading file: ${e.message}"
                fileName = filePath
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = modifier
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
                modifier = Modifier
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
                        modifier = Modifier
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
                        modifier = Modifier
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
                            val area = RSyntaxTextArea().apply {
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

private fun getSyntaxStyleForFile(file: File): String {
    return when (file.extension.lowercase()) {
        "java" -> SyntaxConstants.SYNTAX_STYLE_JAVA
        "kt", "kts" -> SyntaxConstants.SYNTAX_STYLE_KOTLIN
        "js" -> SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT
        "ts" -> SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT
        "py" -> SyntaxConstants.SYNTAX_STYLE_PYTHON
        "xml" -> SyntaxConstants.SYNTAX_STYLE_XML
        "html", "htm" -> SyntaxConstants.SYNTAX_STYLE_HTML
        "css" -> SyntaxConstants.SYNTAX_STYLE_CSS
        "json" -> SyntaxConstants.SYNTAX_STYLE_JSON
        "yaml", "yml" -> SyntaxConstants.SYNTAX_STYLE_YAML
        "md" -> SyntaxConstants.SYNTAX_STYLE_MARKDOWN
        "sql" -> SyntaxConstants.SYNTAX_STYLE_SQL
        "sh", "bash" -> SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL
        "c", "h" -> SyntaxConstants.SYNTAX_STYLE_C
        "cpp", "hpp", "cc", "cxx" -> SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS
        "go" -> SyntaxConstants.SYNTAX_STYLE_GO
        "rs" -> SyntaxConstants.SYNTAX_STYLE_RUST
        "rb" -> SyntaxConstants.SYNTAX_STYLE_RUBY
        "php" -> SyntaxConstants.SYNTAX_STYLE_PHP
        "cs" -> SyntaxConstants.SYNTAX_STYLE_CSHARP
        "scala" -> SyntaxConstants.SYNTAX_STYLE_SCALA
        "gradle" -> SyntaxConstants.SYNTAX_STYLE_GROOVY
        "dockerfile" -> SyntaxConstants.SYNTAX_STYLE_DOCKERFILE
        else -> SyntaxConstants.SYNTAX_STYLE_NONE
    }
}

