package cc.unitmesh.devins.ui.compose.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import java.io.File

@Composable
fun DevInsEditor(
    content: String,
    onContentChange: (String) -> Unit,
    currentFile: File?,
    modifier: Modifier = Modifier
) {
    val richTextState = rememberRichTextState()

    // 当内容改变时更新富文本状态
    LaunchedEffect(content) {
        if (richTextState.annotatedString.text != content) {
            richTextState.setMarkdown(content)
        }
    }

    // 当富文本状态改变时更新内容
    LaunchedEffect(richTextState.annotatedString.text) {
        val newContent = richTextState.toMarkdown()
        if (newContent != content) {
            onContentChange(newContent)
        }
    }
    
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // 编辑器标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val title = if (currentFile != null) {
                    val icon = if (isDevInsFile(currentFile)) "📝" else "📄"
                    "$icon ${currentFile.name}"
                } else {
                    "📝 Editor"
                }
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Divider()
            
            // 富文本编辑器
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (isDevInsFile(currentFile)) {
                    // 对于 DevIns 文件，使用富文本编辑器
                    RichTextEditor(
                        state = richTextState,
                        modifier = Modifier.fillMaxSize(),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        placeholder = {
                            Text(
                                text = "Start typing your DevIns template...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    )
                } else {
                    // 对于其他文件，使用基本文本编辑器
                    BasicTextField(
                        value = content,
                        onValueChange = onContentChange,
                        modifier = Modifier.fillMaxSize(),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            if (content.isEmpty()) {
                                Text(
                                    text = "Start typing...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 14.sp
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }
        }
    }
}

private fun isDevInsFile(file: File?): Boolean {
    if (file == null) return true // 默认情况下使用富文本编辑器
    val extension = file.extension.lowercase()
    return extension in setOf("devin", "devins")
}
