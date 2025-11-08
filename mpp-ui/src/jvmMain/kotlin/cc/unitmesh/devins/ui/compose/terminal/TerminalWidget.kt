package cc.unitmesh.devins.ui.compose.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.jediterm.terminal.TtyConnector
import com.jediterm.terminal.ui.JediTermWidget
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider
import java.awt.Dimension
import java.io.IOException
import java.nio.charset.Charset

/**
 * TtyConnector implementation that wraps a Process (typically from Pty4J).
 * This bridges Pty4J processes to JediTerm's terminal widget.
 */
class ProcessTtyConnector(
    private val process: Process,
    private val charset: Charset = Charsets.UTF_8
) : TtyConnector {

    @Volatile
    private var closed = false

    override fun read(buf: CharArray, offset: Int, length: Int): Int {
        if (closed) return -1

        return try {
            val bytes = ByteArray(length)
            val bytesRead = process.inputStream.read(bytes, 0, length)

            if (bytesRead <= 0) return bytesRead

            val chars = String(bytes, 0, bytesRead, charset).toCharArray()
            System.arraycopy(chars, 0, buf, offset, chars.size.coerceAtMost(length))
            chars.size
        } catch (e: IOException) {
            if (!closed) throw e
            -1
        }
    }

    override fun write(bytes: ByteArray) {
        if (closed) return

        try {
            process.outputStream.write(bytes)
            process.outputStream.flush()
        } catch (e: IOException) {
            if (!closed) throw e
        }
    }

    override fun write(string: String) {
        write(string.toByteArray(charset))
    }

    override fun isConnected(): Boolean {
        return !closed && process.isAlive
    }

    override fun waitFor(): Int {
        return try {
            process.waitFor()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            -1
        }
    }

    override fun ready(): Boolean {
        return !closed && process.isAlive
    }

    override fun close() {
        closed = true
        try {
            process.outputStream.close()
            process.inputStream.close()
            process.errorStream?.close()
            process.destroy()
        } catch (e: IOException) {
            // Ignore
        }
    }

    override fun getName(): String {
        return "AutoDev Pty Process"
    }
}

/**
 * Compose wrapper for JediTerm terminal widget.
 * Bridges Swing-based JediTerm to Compose UI.
 */
@Composable
fun TerminalWidget(
    ttyConnector: TtyConnector?,
    modifier: Modifier = Modifier,
    onTerminalReady: ((JediTermWidget) -> Unit)? = null
) {
    var terminalWidget by remember { mutableStateOf<JediTermWidget?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            terminalWidget?.close()
        }
    }

    SwingPanel(
        modifier = modifier,
        factory = {
            val settingsProvider = DefaultSettingsProvider()
            val widget = JediTermWidget(settingsProvider)

            // Set minimum size to prevent layout issues
            widget.preferredSize = Dimension(800, 400)
            widget.minimumSize = Dimension(400, 200)

            terminalWidget = widget

            // Connect to TTY if provided
            ttyConnector?.let { connector ->
                widget.ttyConnector = connector
                widget.start()
            }

            onTerminalReady?.invoke(widget)

            widget
        },
        update = { widget ->
            // Update terminal if ttyConnector changes
            if (ttyConnector != null && widget.ttyConnector != ttyConnector) {
                widget.ttyConnector = ttyConnector
                widget.start()
            }
        }
    )
}

/**
 * Simple terminal output display for showing command results.
 * This is a read-only terminal view for displaying shell command output.
 *
 * Note: This component is designed to be used inside LazyColumn,
 * so it does NOT include scroll modifiers to avoid nested scrolling conflicts.
 */
@Composable
fun TerminalOutputDisplay(
    output: String,
    modifier: Modifier = Modifier
) {
    // For now, use simple text display instead of JediTerm
    // JediTerm requires more complex setup for read-only output
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(8.dp)
    ) {
        Text(
            text = output,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * JVM implementation of platform-specific terminal display
 */
@Composable
actual fun PlatformTerminalDisplay(
    output: String,
    modifier: Modifier
) {
    TerminalOutputDisplay(output, modifier)
}

