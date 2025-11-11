package cc.unitmesh.devins.ui.compose.terminal

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.agent.tool.shell.PtyShellExecutor
import cc.unitmesh.agent.tool.shell.ShellExecutionConfig
import cc.unitmesh.devins.ui.compose.theme.AutoDevTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Integration component that connects PtyShellExecutor with the ANSI Terminal Renderer.
 * This demonstrates how to execute shell commands and display their output with proper
 * ANSI formatting.
 */
@Composable
fun PtyTerminalIntegration(
    modifier: Modifier = Modifier,
    initialCommand: String = "",
    workingDirectory: File? = null
) {
    var command by remember { mutableStateOf(initialCommand) }
    var isExecuting by remember { mutableStateOf(false) }
    var terminalOutput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val executor = remember { PtyShellExecutor() }

    fun executeCommand() {
        if (command.isBlank() || isExecuting) return

        scope.launch {
            isExecuting = true
            errorMessage = null
            terminalOutput = ""

            try {
                val config = ShellExecutionConfig(
                    workingDirectory = workingDirectory?.absolutePath,
                    timeoutMs = 60000L, // 60 seconds
                    inheritIO = false
                )

                val result = withContext(Dispatchers.IO) {
                    executor.execute(command, config)
                }

                // Build formatted output
                val output = buildString {
                    appendLine("[Shell Command]")
                    appendLine("Command: $command")
                    appendLine("Exit Code: ${result.exitCode}")
                    appendLine("Execution Time: ${result.executionTimeMs}ms")
                    if (result.workingDirectory != null) {
                        appendLine("Working Directory: ${result.workingDirectory}")
                    }
                    appendLine()

                    if (result.exitCode != 0) {
                        append("\u001B[31;1m") // Bold red
                        appendLine("Error: Command failed with exit code: ${result.exitCode}")
                        append("\u001B[0m") // Reset
                        appendLine()
                    }

                    if (result.stdout.isNotEmpty()) {
                        appendLine("Output:")
                        appendLine(result.stdout)
                    }

                    if (result.stderr.isNotEmpty()) {
                        append("\u001B[31m") // Red
                        appendLine("Stderr:")
                        appendLine(result.stderr)
                        append("\u001B[0m") // Reset
                    }
                }

                terminalOutput = output

            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error"
                terminalOutput = buildString {
                    append("\u001B[31;1m") // Bold red
                    appendLine("Exception: ${e.javaClass.simpleName}")
                    append("\u001B[0m") // Reset
                    appendLine(e.message ?: "Unknown error")
                    e.stackTrace.take(5).forEach {
                        appendLine("  at $it")
                    }
                }
            } finally {
                isExecuting = false
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Command input area
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Shell Command Executor",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = command,
                        onValueChange = { command = it },
                        label = { Text("Command") },
                        modifier = Modifier.weight(1f),
                        enabled = !isExecuting,
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { executeCommand() },
                        enabled = !isExecuting && command.isNotBlank()
                    ) {
                        if (isExecuting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Execute")
                        }
                    }
                }

                if (workingDirectory != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Working Directory: ${workingDirectory.absolutePath}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Error: $errorMessage",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Terminal output area
        if (terminalOutput.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                AnsiTerminalRenderer(
                    ansiText = terminalOutput,
                    modifier = Modifier.fillMaxSize(),
                    maxHeight = 800
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Enter a command and click Execute to see the output",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Preview with a sample command.
 */
@Preview
@Composable
fun PtyTerminalIntegrationPreview() {
    AutoDevTheme(darkTheme = true) {
        Surface {
            PtyTerminalIntegration(
                initialCommand = "echo 'Hello, World!'",
                workingDirectory = File(System.getProperty("user.home"))
            )
        }
    }
}

/**
 * Live terminal session that streams output in real-time.
 * This uses LiveShellExecutor for real-time output streaming.
 */
@Composable
fun LivePtyTerminal(
    command: String,
    workingDirectory: File? = null,
    modifier: Modifier = Modifier,
    onComplete: (exitCode: Int) -> Unit = {}
) {
    var appendText by remember { mutableStateOf<((String) -> Unit)?>(null) }
    var clearTerminal by remember { mutableStateOf<(() -> Unit)?>(null) }

    val scope = rememberCoroutineScope()
    val executor = remember { PtyShellExecutor() }

    // Execute command when component is first composed
    LaunchedEffect(command) {
        if (appendText == null) return@LaunchedEffect

        clearTerminal?.invoke()

        try {
            val config = ShellExecutionConfig(
                workingDirectory = workingDirectory?.absolutePath,
                timeoutMs = 300000L, // 5 minutes
                inheritIO = false
            )

            // Start live execution
            val session = withContext(Dispatchers.IO) {
                executor.startLiveExecution(command, config)
            }

            // Stream output
            launch {
                while (!session.isCompleted.value) {
                    val output = session.getStdout()
                    if (output.isNotEmpty()) {
                        appendText?.invoke(output)
                    }
                    kotlinx.coroutines.delay(100)
                }
            }

            // Wait for completion
            val exitCode = withContext(Dispatchers.IO) {
                executor.waitForSession(session, config.timeoutMs)
            }

            // Get any remaining output
            val finalOutput = session.getStdout()
            if (finalOutput.isNotEmpty()) {
                appendText?.invoke(finalOutput)
            }

            onComplete(exitCode)

        } catch (e: Exception) {
            appendText?.invoke("\n\u001B[31;1mError: ${e.message}\u001B[0m\n")
        }
    }

    LiveTerminalRenderer(
        modifier = modifier,
        maxHeight = 600,
        showCursor = true
    ) { append, clear ->
        appendText = append
        clearTerminal = clear
    }
}

/**
 * Preview for live terminal.
 */
@Preview
@Composable
fun LivePtyTerminalPreview() {
    AutoDevTheme(darkTheme = true) {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Live Terminal Output",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                LivePtyTerminal(
                    command = "ls -la",
                    workingDirectory = File(System.getProperty("user.home"))
                )
            }
        }
    }
}

