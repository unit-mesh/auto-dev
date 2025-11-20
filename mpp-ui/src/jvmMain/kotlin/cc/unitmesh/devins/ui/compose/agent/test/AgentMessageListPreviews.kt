package cc.unitmesh.devins.ui.compose.agent.test

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cc.unitmesh.devins.llm.Message
import cc.unitmesh.devins.llm.MessageRole
import cc.unitmesh.devins.ui.compose.agent.AgentMessageList
import cc.unitmesh.devins.ui.compose.agent.CombinedToolItem
import cc.unitmesh.devins.ui.compose.agent.ComposeRenderer
import cc.unitmesh.devins.ui.compose.agent.CurrentToolCallItem
import cc.unitmesh.devins.ui.compose.agent.ToolErrorItem
import cc.unitmesh.devins.ui.compose.agent.MessageItem
import cc.unitmesh.devins.ui.compose.agent.TaskCompletedItem
import cc.unitmesh.devins.ui.compose.agent.TerminalOutputItem
import cc.unitmesh.devins.ui.compose.agent.ToolResultItem

@Preview
@Composable
fun Preview_MessageItem_Assistant() {
    MaterialTheme {
        Surface {
            MessageItem(
                message = Message(
                    MessageRole.ASSISTANT,
                    "This is a sample assistant message for preview. It demonstrates multi-line output and UI layout."
                )
            )
        }
    }
}

@Preview
@Composable
fun Preview_ToolResultItem_Success() {
    MaterialTheme {
        Surface {
            ToolResultItem(
                toolName = "grep",
                success = true,
                summary = "Found matches",
                output = "match1\nmatch2",
                fullOutput = "match1\nmatch2\nmatch3"
            )
        }
    }
}

@Preview
@Composable
fun Preview_ToolResultItem_Failure() {
    MaterialTheme {
        Surface {
            ToolResultItem(
                toolName = "build",
                success = false,
                summary = "Compilation failed",
                output = "Error: missing semicolon at line 32",
                fullOutput = "Error: missing semicolon at line 32\nStacktrace..."
            )
        }
    }
}

@Preview
@Composable
fun Preview_ErrorItem() {
    MaterialTheme {
        Surface {
            ToolErrorItem(error = "Something went wrong: simulated error for preview.") { }
        }
    }
}

@Preview
@Composable
fun Preview_TaskCompletedItem_Success() {
    MaterialTheme {
        Surface {
            TaskCompletedItem(success = true, message = "Task completed successfully")
        }
    }
}

@Preview
@Composable
fun Preview_TaskCompletedItem_Warning() {
    MaterialTheme {
        Surface {
            TaskCompletedItem(success = false, message = "Task completed with warnings")
        }
    }
}

@Preview
@Composable
fun Preview_CurrentToolCallItem() {
    MaterialTheme {
        Surface {
            CurrentToolCallItem(
                toolCall = ComposeRenderer.ToolCallInfo(
                    toolName = "Shell",
                    description = "Executing sample command",
                    details = "Executing: echo hello"
                )
            )
        }
    }
}

@Preview
@Composable
fun Preview_CombinedToolItem_FileRead() {
    MaterialTheme {
        Surface {
            CombinedToolItem(
                toolName = "src/Main.kt - ReadFile",
                details = "path=src/Main.kt",
                fullParams = "path=src/Main.kt encoding=utf-8",
                filePath = "src/Main.kt",
                toolType = null,
                success = true,
                summary = "Read 12 lines",
                output = "fun main() { println(\"hello\") }",
                fullOutput = "fun main() { println(\"hello\") }\n// more lines...",
                executionTimeMs = 5L,
                onOpenFileViewer = { /* preview click noop */ }
            )
        }
    }
}

@Preview
@Composable
fun Preview_TerminalOutputItem_Success() {
    MaterialTheme {
        Surface {
            TerminalOutputItem(
                command = "ls -la",
                output = "file1\nfile2\nfile3",
                exitCode = 0,
                executionTimeMs = 42L
            )
        }
    }
}

@Preview
@Composable
fun Preview_TerminalOutputItem_Failure() {
    MaterialTheme {
        Surface {
            TerminalOutputItem(
                command = "cat missing-file.txt",
                output = "cat: missing-file.txt: No such file or directory",
                exitCode = 1,
                executionTimeMs = 10L
            )
        }
    }
}

@Preview
@Composable
fun Preview_AgentMessageList_Full() {
    val renderer = ComposeRenderer().apply {
        clearMessages()
        addUserMessage("Please search for TODOs in the project")
        // Simulate streaming LLM output
        renderLLMResponseStart()
        renderLLMResponseChunk("Searching for TODOs...")
        renderLLMResponseEnd()
        // Simulate a tool call and result
        renderToolCall("glob", "pattern=**/*.kt")
        renderToolResult("glob", true, "Found 2 files matching pattern", null, mapOf("execution_time_ms" to "12"))
        // Simulate terminal output
        renderToolCall("shell", "command=echo hello")
        renderToolResult("shell", true, "hello\n", null, mapOf("execution_time_ms" to "5"))
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AgentMessageList(renderer = renderer, modifier = Modifier.fillMaxSize(), onOpenFileViewer = { /* noop */ })
        }
    }
}

