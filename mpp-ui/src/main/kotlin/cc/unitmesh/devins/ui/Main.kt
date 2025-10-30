package cc.unitmesh.devins.ui

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.compose.DevInsApp

fun main() = application {
    val windowState = rememberWindowState(
        width = 1400.dp,
        height = 900.dp
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "AutoDev Desktop",
        state = windowState
    ) {
        DevInsApp()
    }
}
