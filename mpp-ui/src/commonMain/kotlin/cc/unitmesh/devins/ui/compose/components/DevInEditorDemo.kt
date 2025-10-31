package cc.unitmesh.devins.ui.compose.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.compose.editor.DevInEditorInput
import cc.unitmesh.devins.ui.compose.editor.model.EditorCallbacks

/**
 * DevIn 编辑器演示页面
 * 用于测试和展示编辑器的各项功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevInEditorDemo() {
    var submittedText by remember { mutableStateOf("") }
    var lastChangedText by remember { mutableStateOf("") }
    var eventLog by remember { mutableStateOf<List<String>>(emptyList()) }
    
    val callbacks = object : EditorCallbacks {
        override fun onSubmit(text: String) {
            submittedText = text
            eventLog = eventLog + "✅ Submitted: ${text.take(50)}${if (text.length > 50) "..." else ""}"
        }
        
        override fun onTextChanged(text: String) {
            lastChangedText = text
        }
        
        override fun onCursorMoved(position: Int) {
            eventLog = eventLog + "📍 Cursor moved to: $position"
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DevIn Editor Demo") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 左侧：编辑器
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 标题和说明
                Text(
                    text = "DevIn Editor",
                    style = MaterialTheme.typography.headlineMedium
                )
                
                Text(
                    text = "Try typing:",
                    style = MaterialTheme.typography.titleSmall
                )
                
                // 功能提示卡片
                FeatureHintCard()
                
                // 编辑器
                DevInEditorInput(
                    initialText = getExampleText(),
                    placeholder = "Type @ for agents, / for commands, \$ for variables...",
                    callbacks = callbacks,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // 提交的内容
                if (submittedText.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Last Submitted:",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = submittedText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
            
            // 右侧：调试信息和示例
            Column(
                modifier = Modifier
                    .width(400.dp)
                    .fillMaxHeight()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 事件日志
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Event Log",
                                style = MaterialTheme.typography.titleMedium
                            )
                            TextButton(
                                onClick = { eventLog = emptyList() }
                            ) {
                                Text("Clear")
                            }
                        }
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        if (eventLog.isEmpty()) {
                            Text(
                                text = "No events yet...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                eventLog.takeLast(10).forEach { event ->
                                    Text(
                                        text = event,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
                
                // 示例代码
                ExamplesCard()
                
                // 快捷键说明
                ShortcutsCard()
            }
        }
    }
}

@Composable
private fun FeatureHintCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("• Type @ to trigger agent completion", style = MaterialTheme.typography.bodySmall)
            Text("• Type / to trigger command completion", style = MaterialTheme.typography.bodySmall)
            Text("• Type \$ to trigger variable completion", style = MaterialTheme.typography.bodySmall)
            Text("• Press Enter to submit", style = MaterialTheme.typography.bodySmall)
            Text("• Press Shift+Enter to insert new line", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ExamplesCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Examples",
                style = MaterialTheme.typography.titleMedium
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            ExampleItem(
                title = "Agent Usage",
                code = "@clarify What is the purpose of this function?"
            )
            ExampleItem(
                title = "Command Usage",
                code = "/file:src/main/kotlin/Main.kt"
            )
            ExampleItem(
                title = "Variable Usage",
                code = "The value is \$input"
            )
            ExampleItem(
                title = "Combined",
                code = """
                    @code-review
                    /file:src/main/kotlin/Main.kt
                    Please review this file and check for issues
                """.trimIndent()
            )
        }
    }
}

@Composable
private fun ExampleItem(title: String, code: String) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = code,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
private fun ShortcutsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Keyboard Shortcuts",
                style = MaterialTheme.typography.titleMedium
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            ShortcutItem("↵", "Submit")
            ShortcutItem("⇧↵", "New line")
            ShortcutItem("⌃↵ / ⌘↵", "New line (Alt)")
            ShortcutItem("↓ / ↑", "Navigate completion")
            ShortcutItem("⇥ / ↵", "Accept completion")
            ShortcutItem("Esc", "Dismiss completion")
        }
    }
}

@Composable
private fun ShortcutItem(shortcut: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = shortcut,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun getExampleText(): String = """
---
name: "Example Template"
variables:
  input: "sample"
  output: "result"
---

# DevIn Template Example

@clarify What should we do here?

/file:src/main/kotlin/Main.kt

Process the ${"$"}input and generate ${"$"}output.
""".trimIndent()

