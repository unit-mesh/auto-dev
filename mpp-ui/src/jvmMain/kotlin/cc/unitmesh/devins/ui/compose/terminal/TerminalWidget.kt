package cc.unitmesh.devins.ui.compose.terminal

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.jediterm.terminal.TtyConnector
import com.jediterm.terminal.ui.JediTermWidget
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider
import java.awt.Dimension

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
 */
@Composable
fun TerminalOutputDisplay(
    output: String,
    modifier: Modifier = Modifier
) {
    SwingPanel(
        modifier = modifier,
        factory = {
            val settingsProvider = DefaultSettingsProvider()
            val widget = JediTermWidget(settingsProvider)

            widget.preferredSize = Dimension(800, 300)
            widget.minimumSize = Dimension(400, 150)

            // Display the output in the terminal
//            widget.terminalTextBuffer.writeString(0, 0, CharBuffer.wrap(output))

            widget
        }
    )
}

