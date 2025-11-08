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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.jediterm.terminal.TtyConnector
import com.jediterm.terminal.ui.JediTermWidget
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider
import java.awt.Dimension
import java.io.IOException
import java.nio.charset.Charset
import java.util.function.Supplier
import javax.swing.JScrollBar

/**
 * Custom JediTerm settings provider that integrates with Compose Material Theme.
 * Inspired by IDEA's JBTerminalSystemSettingsProvider implementation.
 */
class ComposeTerminalSettingsProvider(
    private val backgroundColor: Color,
    private val foregroundColor: Color,
    private val selectionColor: Color,
    private val cursorColor: Color = Color(0xFF64B5F6) // Material blue by default
) : DefaultSettingsProvider() {

    // Convert Compose Color to JediTerm Color supplier
    private fun Color.toJediColorSupplier(): Supplier<com.jediterm.core.Color> {
        val argb = this.toArgb()
        val jediColor = com.jediterm.core.Color(
            (argb shr 16) and 0xFF,  // red
            (argb shr 8) and 0xFF,   // green
            argb and 0xFF             // blue
        )
        return Supplier { jediColor }
    }

    override fun getDefaultStyle(): com.jediterm.terminal.TextStyle {
        return com.jediterm.terminal.TextStyle(
            com.jediterm.terminal.TerminalColor(foregroundColor.toJediColorSupplier()),
            com.jediterm.terminal.TerminalColor(backgroundColor.toJediColorSupplier())
        )
    }

    override fun getFoundPatternColor(): com.jediterm.terminal.TextStyle {
        return com.jediterm.terminal.TextStyle(
            com.jediterm.terminal.TerminalColor(foregroundColor.toJediColorSupplier()),
            com.jediterm.terminal.TerminalColor(selectionColor.toJediColorSupplier())
        )
    }

    override fun getSelectionColor(): com.jediterm.terminal.TextStyle {
        return com.jediterm.terminal.TextStyle(
            com.jediterm.terminal.TerminalColor(foregroundColor.toJediColorSupplier()),
            com.jediterm.terminal.TerminalColor(selectionColor.toJediColorSupplier())
        )
    }

    // Use the same font settings as IDEA
    override fun useAntialiasing(): Boolean = true

    override fun maxRefreshRate(): Int = 50

    // Enable modern terminal features
    override fun audibleBell(): Boolean = false

    override fun copyOnSelect(): Boolean = true

    override fun pasteOnMiddleMouseClick(): Boolean = true
}/**
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
/**
 * Custom JediTermWidget that overrides scrollbar creation like IDEA's JBTerminalWidget.
 * Following IDEA's pattern: createScrollBar() is called during parent constructor,
 * so we use a lazy approach to access terminal panel colors after initialization.
 */
class AutoDevTerminalWidget(
    settingsProvider: ComposeTerminalSettingsProvider
) : JediTermWidget(settingsProvider) {

    override fun createScrollBar(): JScrollBar {
        // Like JBTerminalWidget, create scrollbar that adapts to terminal panel background
        // Use anonymous class like IDEA does to access terminalPanel after initialization
        val bar = object : ModernTerminalScrollBar(
            VERTICAL,
            TerminalScrollbarColors(
                track = java.awt.Color(30, 30, 30, 20),
                thumb = java.awt.Color(100, 181, 246, 140),
                thumbHover = java.awt.Color(100, 181, 246)
            )
        ) {
            override fun getBackground(): java.awt.Color {
                // Return terminal panel background like JBScrollBar does
                return terminalPanel?.background ?: super.getBackground()
            }
        }

        bar.isOpaque = true
        bar.unitIncrement = 10
        bar.blockIncrement = 48
        return bar
    }
}

/**
 * Modern terminal widget component with Material Theme integration.
 * Inspired by IntelliJ IDEA's JBTerminalWidget implementation.
 *
 * Features:
 * - Material3 color scheme integration
 * - Custom styled scrollbar via createScrollBar() override
 * - Antialiasing and modern rendering
 * - Copy on select and paste on middle click
 */
@Composable
fun TerminalWidget(
    ttyConnector: TtyConnector?,
    modifier: Modifier = Modifier,
    onTerminalReady: ((JediTermWidget) -> Unit)? = null
) {
    var terminalWidget by remember { mutableStateOf<JediTermWidget?>(null) }

    // Get Material Theme colors
    val backgroundColor = MaterialTheme.colorScheme.surface
    val foregroundColor = MaterialTheme.colorScheme.onSurface
    val selectionColor = MaterialTheme.colorScheme.primaryContainer
    val cursorColor = MaterialTheme.colorScheme.primary
    val primaryColor = MaterialTheme.colorScheme.primary

    DisposableEffect(Unit) {
        onDispose {
            terminalWidget?.close()
        }
    }

    SwingPanel(
        modifier = modifier,
        background = backgroundColor,
        factory = {
            val settingsProvider = ComposeTerminalSettingsProvider(
                backgroundColor = backgroundColor,
                foregroundColor = foregroundColor,
                selectionColor = selectionColor,
                cursorColor = cursorColor
            )

            // Create custom terminal widget with overridden createScrollBar()
            // No need to pass colors - widget will extract from settingsProvider
            val widget = AutoDevTerminalWidget(settingsProvider)

            // Set size constraints
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
}/**
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

