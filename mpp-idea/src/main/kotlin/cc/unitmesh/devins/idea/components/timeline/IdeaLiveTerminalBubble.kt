package cc.unitmesh.devins.idea.components.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.agent.render.TimelineItem
import cc.unitmesh.agent.tool.shell.ShellSessionManager
import cc.unitmesh.devins.idea.renderer.terminal.IdeaAnsiTerminalRenderer
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text

/**
 * Process output state for UI consumption
 */
data class ProcessOutputState(
    val output: String = "",
    val isRunning: Boolean = true,
    val exitCode: Int? = null
)

/**
 * Collector that monitors a Process and emits output updates via Flow.
 * Uses a listener-like pattern with periodic checks.
 *
 * Also syncs output to ShellSessionManager for cancel event handling.
 */
class ProcessOutputCollector(
    private val process: Process,
    private val sessionId: String? = null,
    private val checkIntervalMs: Long = 100L
) {
    private val _state = MutableStateFlow(ProcessOutputState())
    val state: StateFlow<ProcessOutputState> = _state.asStateFlow()

    private val buffer = StringBuilder()
    private var job: Job? = null

    /**
     * Start collecting output from the process.
     * Call this from a coroutine scope.
     */
    fun start(scope: CoroutineScope) {
        job = scope.launch(Dispatchers.IO) {
            try {
                // PTY processes combine stdout and stderr into a single stream (inputStream)
                // Reading both streams simultaneously would cause issues
                val outputJob = launch { readStream(process.inputStream) }

                // Periodic check for process completion
                while (isActive && process.isAlive) {
                    delay(checkIntervalMs)
                }

                // Process ended - wait a bit for streams to flush
                delay(50)
                outputJob.cancel()

                // Update final state
                _state.update { it.copy(
                    output = buffer.toString(),
                    isRunning = false,
                    exitCode = process.exitValue()
                )}
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                buffer.append("\n\u001B[31mError: ${e.message}\u001B[0m")
                _state.update { it.copy(
                    output = buffer.toString(),
                    isRunning = false
                )}
            }
        }
    }

    private suspend fun readStream(stream: java.io.InputStream) {
        try {
            val reader = stream.bufferedReader()
            val charBuffer = CharArray(1024)
            while (currentCoroutineContext().isActive) {
                val bytesRead = reader.read(charBuffer)
                if (bytesRead == -1) break

                val chunk = String(charBuffer, 0, bytesRead)
                synchronized(buffer) {
                    buffer.append(chunk)
                }

                _state.update { it.copy(output = buffer.toString()) }

                // Sync to ShellSessionManager for cancel event handling
                sessionId?.let { sid ->
                    ShellSessionManager.getSession(sid)?.appendOutput(chunk)
                }
            }
        } catch (e: Exception) {
            // Stream closed
        }
    }

    fun stop() {
        job?.cancel()
    }

    /**
     * Get current output buffer content
     */
    fun getCurrentOutput(): String = synchronized(buffer) { buffer.toString() }
}

/**
 * Data class for cancel event with session info and output log.
 */
data class CancelEvent(
    val sessionId: String,
    val command: String,
    val output: String,
    val process: Process
)

/**
 * Live terminal bubble for displaying real-time shell command output.
 * Uses ProcessOutputCollector for listener-like output monitoring.
 *
 * Features:
 * - Real-time output streaming via Flow
 * - ANSI color and formatting support
 * - Collapsible output with header
 * - Status indicator (running/completed)
 * - Cancel button to terminate running process
 * - On cancel, sends current output log to AI
 */
@Composable
fun IdeaLiveTerminalBubble(
    item: TimelineItem.LiveTerminalItem,
    modifier: Modifier = Modifier,
    project: Project? = null,
    onCancel: ((CancelEvent) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(true) }

    val process = remember(item.ptyHandle) { item.ptyHandle as? Process }

    // Create collector and collect state - pass sessionId for sync to ShellSessionManager
    val collector = remember(process, item.sessionId) {
        process?.let { ProcessOutputCollector(it, sessionId = item.sessionId) }
    }

    val outputState by collector?.state?.collectAsState()
        ?: remember { mutableStateOf(ProcessOutputState(
            output = "[No process handle available]",
            isRunning = false
        )) }

    // Start collector when process is available
    val scope = rememberCoroutineScope()
    LaunchedEffect(collector) {
        collector?.start(scope)
    }

    // Cleanup on dispose
    DisposableEffect(collector) {
        onDispose { collector?.stop() }
    }

    // Override with external exitCode if provided
    val actualExitCode = item.exitCode ?: outputState.exitCode
    val isRunning = if (item.exitCode != null) false else outputState.isRunning
    val output = outputState.output.ifEmpty { "Waiting for output..." }

    // Cancel handler - sends current output log to AI before terminating
    val handleCancel: () -> Unit = {
        process?.let { p ->
            // Get current output directly from collector's buffer (more reliable than state)
            val currentOutput = collector?.getCurrentOutput() ?: outputState.output

            // Create cancel event with session info and output
            val cancelEvent = CancelEvent(
                sessionId = item.sessionId,
                command = item.command,
                output = currentOutput,
                process = p
            )

            if (onCancel != null) {
                onCancel(cancelEvent)
            } else {
                // Default: just destroy the process
                p.destroyForcibly()
            }
            collector?.stop()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AutoDevColors.Neutral.c900, RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isRunning) AutoDevColors.Green.c400
                        else if (actualExitCode == 0) AutoDevColors.Green.c400
                        else AutoDevColors.Red.c400
                    )
            )

            // Terminal icon
            Text(
                text = "💻",
                style = JewelTheme.defaultTextStyle.copy(fontSize = 14.sp)
            )

            // Command
            Text(
                text = item.command,
                style = JewelTheme.defaultTextStyle.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = AutoDevColors.Cyan.c400
                ),
                modifier = Modifier.weight(1f),
                maxLines = 1
            )

            // Status badge
            val (statusText, statusColor) = when {
                isRunning -> "RUNNING" to AutoDevColors.Green.c400
                actualExitCode == 0 -> "EXIT 0" to AutoDevColors.Green.c400
                else -> "EXIT ${actualExitCode ?: "?"}" to AutoDevColors.Red.c400
            }

            Box(
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = statusText,
                    style = JewelTheme.defaultTextStyle.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                )
            }
        }

        // Working directory
        if (item.workingDirectory != null) {
            Text(
                text = "📁 ${item.workingDirectory}",
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = AutoDevColors.Neutral.c400
                ),
                modifier = Modifier.padding(start = 16.dp, top = 2.dp)
            )
        }

        // Collapsible output with cancel button
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Output content
                if (output.isNotEmpty()) {
                    IdeaAnsiTerminalRenderer(
                        ansiText = output,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .heightIn(min = 60.dp, max = 300.dp),
                        maxHeight = 300,
                        backgroundColor = AutoDevColors.Neutral.c900
                    )
                } else if (isRunning) {
                    Text(
                        text = "Waiting for output...",
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 11.sp,
                            color = AutoDevColors.Neutral.c400
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Cancel button - only show when running
                if (isRunning && process != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(
                                AutoDevColors.Red.c600.copy(alpha = 0.9f),
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { handleCancel() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            style = JewelTheme.defaultTextStyle.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = AutoDevColors.Neutral.c50
                            )
                        )
                    }
                }
            }
        }
    }
}

