package cc.unitmesh.devins.ui.compose.agent.test

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cc.unitmesh.devins.ui.compose.agent.AgentMessageList
import cc.unitmesh.devins.ui.compose.agent.ComposeRenderer
import cc.unitmesh.devins.ui.compose.theme.AutoDevTheme
import cc.unitmesh.devins.ui.compose.theme.ThemeManager

fun main() =
    application {
        val windowState =
            rememberWindowState(
                width = 1000.dp,
                height = 800.dp
            )

        Window(
            onCloseRequest = ::exitApplication,
            title = "AgentMessageList Preview Test",
            state = windowState
        ) {
            AutoDevTheme(themeMode = ThemeManager.ThemeMode.SYSTEM) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AgentMessageListPreviewScreen()
                }
            }
        }
    }

@Composable
@Preview
fun AgentMessageListPreviewScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        AgentMessageListPreview()
    }
}

@Composable
fun AgentMessageListPreview(modifier: Modifier = Modifier) {
    val mockRenderer = createMockRenderer()

    AgentMessageList(
        renderer = mockRenderer,
        modifier = modifier.fillMaxSize(),
        onOpenFileViewer = { filePath ->
            println("Opening file viewer for: $filePath")
        }
    )
}

/**
 * Creates a mock ComposeRenderer with simulated timeline data
 * representing a complete CodingAgent workflow
 *
 * This simulates the typical flow:
 * User Task → Agent Reasoning → Tool Calls → Results → More Reasoning → Completion
 */
private fun createMockRenderer(): ComposeRenderer {
    val renderer = ComposeRenderer()

    simulateAgentReasoning(
        renderer,
        """I'll help you add a sum calculation feature to the MathUtils class. Let me start by:

1. First, I'll read the existing MathUtils.kt file to understand its current structure
2. Then, I'll add the new sum function
3. Finally, I'll run the tests to ensure everything works

Let's begin!"""
    )

    renderer.renderToolCall("read-file", """path="src/main/kotlin/utils/MathUtils.kt" startLine=1 endLine=50""")
    renderer.renderToolResult(
        "read-file",
        true,
        "Read 25 lines",
        fullOutput = """1  │ package utils
2  │
3  │ /**
4  │  * Mathematical utility functions
5  │  */
6  │ object MathUtils {
7  │     /**
8  │      * Multiplies two integers
9  │      */
10 │     fun multiply(a: Int, b: Int): Int {
11 │         return a * b
12 │     }
13 │
14 │     /**
15 │      * Divides two integers
16 │      * @throws IllegalArgumentException if divisor is zero
17 │      */
18 │     fun divide(a: Int, b: Int): Int {
19 │         if (b == 0) throw IllegalArgumentException("Cannot divide by zero")
20 │         return a / b
21 │     }
22 │ }"""
    )

    // Iteration 2: Add sum function
    simulateAgentReasoning(
        renderer,
        """Perfect! I can see the MathUtils class currently has `multiply` and `divide` functions.

Now I'll add a new `sum` function that follows the same pattern. The function will be simple and clean."""
    )

    renderer.renderToolCall("write-file", """path="src/main/kotlin/utils/MathUtils.kt" mode="update" content="<content>"""")
    renderer.renderToolResult(
        "write-file",
        true,
        "File updated successfully",
        fullOutput = "✓ Updated src/main/kotlin/utils/MathUtils.kt (30 lines)"
    )

    renderer.renderFinalResult(true, "Task completed successfully after 6 iterations", 6)

    return renderer
}

private fun simulateAgentReasoning(renderer: ComposeRenderer, content: String) {
    renderer.renderLLMResponseStart()
    renderer.renderLLMResponseChunk(content)
    renderer.renderLLMResponseEnd()
}
