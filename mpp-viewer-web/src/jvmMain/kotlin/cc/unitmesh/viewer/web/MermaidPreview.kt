package cc.unitmesh.viewer.web

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Mermaid Renderer Test",
        state = rememberWindowState(width = 1200.dp, height = 800.dp)
    ) {
        val examples = """
                graph TD
                    A[Start] --> B{Is it working?}
                    B -->|Yes| C[Great!]
                    B -->|No| D[Debug]
                    C --> E[End]
                    D --> B
            """.trimIndent()
        MaterialTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    MermaidRenderer(
                        mermaidCode = examples,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}


