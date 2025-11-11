package cc.unitmesh.devins.ui.compose.editor.test

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cc.unitmesh.devins.completion.CompletionManager
import cc.unitmesh.devins.editor.EditorCallbacks
import cc.unitmesh.devins.ui.compose.editor.DevInEditorInput
import cc.unitmesh.devins.ui.compose.theme.AutoDevTheme
import cc.unitmesh.devins.ui.compose.theme.ThemeManager
import cc.unitmesh.llm.ModelConfig
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch

/**
 * Preview window for DevInEditorInput component
 * Run this main function to test the editor in a standalone window
 */
fun main() =
    application {
        val windowState =
            rememberWindowState(
                width = 800.dp,
                height = 600.dp
            )

        Window(
            onCloseRequest = ::exitApplication,
            title = "DevInEditorInput Preview",
            state = windowState
        ) {
            AutoDevTheme(themeMode = ThemeManager.ThemeMode.SYSTEM) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DevInEditorInputPreviewScreen()
                }
            }
        }
    }

@OptIn(DelicateCoroutinesApi::class)
@Composable
@Preview
fun DevInEditorInputPreviewScreen() {
    var submittedText by remember { mutableStateOf("") }
    var isExecuting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Text(
            text = "DevInEditorInput Preview",
            style = MaterialTheme.typography.headlineMedium
        )

        // Display submitted text
        if (submittedText.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Last Submitted:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = submittedText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Compact mode preview
        Text(
            text = "Compact Mode:",
            style = MaterialTheme.typography.titleMedium
        )
        DevInEditorInputPreview(
            isCompactMode = true,
            isExecuting = isExecuting,
            onSubmit = { text ->
                submittedText = text
                isExecuting = true
                // Simulate execution
                kotlinx.coroutines.GlobalScope.launch {
                    kotlinx.coroutines.delay(2000)
                    isExecuting = false
                }
            },
            onStop = {
                isExecuting = false
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Normal mode preview
        Text(
            text = "Normal Mode:",
            style = MaterialTheme.typography.titleMedium
        )
        DevInEditorInputPreview(
            isCompactMode = false,
            isExecuting = isExecuting,
            onSubmit = { text ->
                submittedText = text
                isExecuting = true
                // Simulate execution
                kotlinx.coroutines.GlobalScope.launch {
                    kotlinx.coroutines.delay(2000)
                    isExecuting = false
                }
            },
            onStop = {
                isExecuting = false
            }
        )
    }
}

@Composable
fun DevInEditorInputPreview(
    isCompactMode: Boolean = false,
    isExecuting: Boolean = false,
    onSubmit: (String) -> Unit = {},
    onStop: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val callbacks = remember {
        object : EditorCallbacks {
            override fun onSubmit(text: String) {
                println("📝 Submitted: $text")
                onSubmit(text)
            }

            override fun onTextChanged(text: String) {
                println("✏️ Text changed: ${text.take(50)}${if (text.length > 50) "..." else ""}")
            }
        }
    }

    val completionManager = remember { CompletionManager() }

    DevInEditorInput(
        initialText = "",
        placeholder = "Type your message... (Try typing / or @ for completions)",
        callbacks = callbacks,
        completionManager = completionManager,
        isCompactMode = isCompactMode,
        isExecuting = isExecuting,
        onStopClick = {
            println("⏹️ Stop clicked")
            onStop()
        },
        modifier = modifier,
        onModelConfigChange = { config ->
            println("🔧 Model config changed: ${config.modelName}")
        }
    )
}

