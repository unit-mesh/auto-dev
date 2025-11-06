package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.dp
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

    LaunchedEffect(filePath) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                fileName = file.name
                errorMessage = null
            } else {
                errorMessage = "File not found: $filePath"
                fileName = filePath
            }
        } catch (e: Exception) {
            errorMessage = "Error: ${e.message}"
            fileName = filePath
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
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Content
        Box(modifier = Modifier.fillMaxSize()) {
            if (errorMessage != null) {
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
            } else {
                // RSyntaxTextArea content
                SwingPanel(
                    background = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxSize(),
                    factory = {
                        val file = File(filePath)
                        val area = RSyntaxTextArea().apply {
                            text = if (file.exists()) file.readText() else ""
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
                        // Update text if file path changes
                        textArea?.let { area ->
                            val file = File(filePath)
                            if (file.exists()) {
                                area.text = file.readText()
                                area.syntaxEditingStyle = getSyntaxStyleForFile(file)
                            }
                        }
                    }
                )
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

