package cc.unitmesh.viewer.web

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
        MermaidTestApp()
    }
}

@Composable
fun MermaidTestApp() {
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

@Preview
@Composable
fun MermaidTestAppPreview() {
    MermaidTestApp()
}


