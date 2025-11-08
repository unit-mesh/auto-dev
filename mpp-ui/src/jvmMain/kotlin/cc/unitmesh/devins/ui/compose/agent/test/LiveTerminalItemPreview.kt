package cc.unitmesh.devins.ui.compose.agent.test

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cc.unitmesh.devins.ui.compose.agent.LiveTerminalItem
import cc.unitmesh.devins.ui.compose.theme.AutoDevTheme
import cc.unitmesh.devins.ui.compose.theme.ThemeManager
import com.pty4j.PtyProcessBuilder

/**
 * Preview + runnable test window for LiveTerminalItem (JVM).
 * Spawns a short-lived PTY process that prints lines with delays so RUNNING state is visible for a bit.
 */
fun main() = application {
    val windowState = rememberWindowState(width = 900.dp, height = 600.dp)
    Window(onCloseRequest = ::exitApplication, title = "LiveTerminalItem Preview", state = windowState) {
        AutoDevTheme(themeMode = ThemeManager.ThemeMode.SYSTEM) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                LiveTerminalItemPreviewScreen()
            }
        }
    }
}

@Composable
@Preview
fun LiveTerminalItemPreviewScreen() {
    // Command simulates incremental output then sleeps to keep process alive
    val commandArray = arrayOf(
        "bash", "-c",
        // Escape $i so Kotlin doesn't try to interpolate; keep process alive briefly
        "for i in $(seq 1 8); do echo Live line \$i; sleep 0.3; done; echo 'Finished.'; sleep 3"
    )
    val ptyHandle = remember {
        PtyProcessBuilder().setCommand(commandArray).start()
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        // Single LiveTerminalItem instance
        LiveTerminalItem(
            sessionId = "preview-live-terminal-${System.currentTimeMillis()}",
            command = commandArray.joinToString(" "),
            workingDirectory = "/project/root",
            ptyHandle = ptyHandle
        )
        Spacer(Modifier.height(12.dp))
        // A second instance to visually validate adaptive height behavior within vertical space
        LiveTerminalItem(
            sessionId = "preview-live-terminal-2-${System.currentTimeMillis()}",
            command = "echo 'second instance'",
            workingDirectory = "/project/root",
            ptyHandle = PtyProcessBuilder().setCommand(arrayOf("echo", "second instance" )).start()
        )
    }
}
