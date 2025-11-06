package cc.unitmesh.devins.ui.compose.agent

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rtextarea.RTextScrollPane
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.File
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * JVM implementation of FileViewer using RSyntaxTextArea
 */
actual class FileViewer {
    private var frame: JFrame? = null

    actual fun showFile(filePath: String, readOnly: Boolean) {
        SwingUtilities.invokeLater {
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    println("File not found: $filePath")
                    return@invokeLater
                }

                // Close existing frame if any
                close()

                // Create text area with syntax highlighting
                val textArea = RSyntaxTextArea().apply {
                    text = file.readText()
                    isEditable = !readOnly
                    syntaxEditingStyle = getSyntaxStyleForFile(file)
                    isCodeFoldingEnabled = true
                    antiAliasingEnabled = true
                    tabSize = 4
                    margin = java.awt.Insets(5, 5, 5, 5)
                }

                // Wrap in scroll pane
                val scrollPane = RTextScrollPane(textArea).apply {
                    isFoldIndicatorEnabled = true
                }

                // Create frame
                frame = JFrame("File Viewer - ${file.name}").apply {
                    defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
                    
                    val panel = JPanel(BorderLayout()).apply {
                        add(scrollPane, BorderLayout.CENTER)
                    }
                    
                    contentPane = panel
                    size = Dimension(900, 700)
                    setLocationRelativeTo(null) // Center on screen
                    isVisible = true
                }
            } catch (e: Exception) {
                println("Error opening file: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    actual fun close() {
        SwingUtilities.invokeLater {
            frame?.dispose()
            frame = null
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
}

