package cc.unitmesh.devins.ui.compose.agent.test

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

fun main() = application {
    val windowState = rememberWindowState(
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

    // Simulate user message
    simulateUserMessage(renderer)

    // Iteration 1: Read existing code
    simulateAgentReasoning(renderer, """I'll help you add a sum calculation feature to the MathUtils class. Let me start by:

1. First, I'll read the existing MathUtils.kt file to understand its current structure
2. Then, I'll add the new sum function
3. Finally, I'll run the tests to ensure everything works

Let's begin!""")

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
    simulateAgentReasoning(renderer, """Perfect! I can see the MathUtils class currently has `multiply` and `divide` functions.

Now I'll add a new `sum` function that follows the same pattern. The function will be simple and clean.""")

    renderer.renderToolCall("write-file", """path="src/main/kotlin/utils/MathUtils.kt" mode="update" content="<content>"""")
    renderer.renderToolResult(
        "write-file",
        true,
        "File updated successfully",
        fullOutput = "✓ Updated src/main/kotlin/utils/MathUtils.kt (30 lines)"
    )

    // Iteration 3: Run tests (with error)
    simulateAgentReasoning(renderer, """Great! I've successfully added the `sum` function. Now let me run the tests to make sure everything is working correctly.""")

    renderer.renderToolCall("shell", """command="./gradlew test --tests MathUtilsTest" workingDir="/project/root"""")
    renderer.renderToolResult(
        "shell",
        false,
        "Test failed (exit code: 1)",
        fullOutput = """> Task :test

MathUtilsTest > testDivide() PASSED
MathUtilsTest > testMultiply() PASSED
MathUtilsTest > testSum() FAILED
    java.lang.AssertionError: expected:<5> but was:<6>
        at org.junit.Assert.fail(Assert.java:89)
        at org.junit.Assert.failNotEquals(Assert.java:835)
        at MathUtilsTest.testSum(MathUtilsTest.kt:15)

3 tests completed, 1 failed

BUILD FAILED in 2s
3 actionable tasks: 2 executed, 1 up-to-date

FAILURE: Build failed with an exception."""
    )

    // Show error and recovery
    renderer.renderError("""Test execution failed: MathUtilsTest > testSum() FAILED

Error Recovery Analysis:
The test is expecting 5 but getting 6. This suggests the test case itself might have an incorrect assertion.
Let me read the test file to investigate the issue.""")

    // Iteration 4: Debug test file
    renderer.renderToolCall("read-file", """path="src/test/kotlin/MathUtilsTest.kt" startLine=1 endLine=30""")
    renderer.renderToolResult(
        "read-file",
        true,
        "Read 18 lines",
        fullOutput = """12 │ @Test
13 │ fun testSum() {
14 │     val result = MathUtils.sum(2, 3)
15 │     assertEquals(6, result) // BUG: Should be 5, not 6!
16 │ }"""
    )

    simulateAgentReasoning(renderer, """Found the issue! The test has an incorrect assertion. Line 15 expects 6, but 2 + 3 = 5.

I'll fix the test case now.""")

    // Iteration 5: Fix test and re-run
    renderer.renderToolCall("write-file", """path="src/test/kotlin/MathUtilsTest.kt" mode="update" content="<fixed test>"""")
    renderer.renderToolResult(
        "write-file",
        true,
        "Test file updated",
        fullOutput = "✓ Updated src/test/kotlin/MathUtilsTest.kt"
    )

    renderer.renderToolCall("shell", """command="./gradlew test --tests MathUtilsTest"""")
    renderer.renderToolResult(
        "shell",
        true,
        "All tests passed ✓",
        fullOutput = """BUILD SUCCESSFUL in 2s
3 actionable tasks: 2 executed, 1 up-to-date

MathUtilsTest > testDivide() PASSED
MathUtilsTest > testMultiply() PASSED
MathUtilsTest > testSum() PASSED

3 tests completed, 0 failed"""
    )

    // Final message
    simulateAgentReasoning(renderer, """Perfect! All tests are now passing. ✅

**Summary of changes:**
- ✓ Added `sum(a: Int, b: Int): Int` function to MathUtils
- ✓ Fixed incorrect test assertion in MathUtilsTest
- ✓ All tests passing (3/3)

The implementation is complete and working correctly!""")

    // Task complete
    renderer.renderFinalResult(true, "Task completed successfully after 5 iterations", 5)

    return renderer
}

/**
 * Helper to simulate user message (uses internal timeline manipulation)
 */
private fun simulateUserMessage(renderer: ComposeRenderer) {
    // We'll use renderLLMResponseStart/Chunk/End to add assistant messages
    // For user messages, we need to access the timeline directly via reflection or a workaround
    // Since there's no public API for user messages, we'll just start with assistant responses
}

/**
 * Helper to simulate agent reasoning (assistant message)
 */
private fun simulateAgentReasoning(renderer: ComposeRenderer, content: String) {
    renderer.renderLLMResponseStart()
    renderer.renderLLMResponseChunk(content)
    renderer.renderLLMResponseEnd()
}
