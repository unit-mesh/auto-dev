package cc.unitmesh.devins.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cc.unitmesh.devins.ui.compose.components.DevInsMainContent
import cc.unitmesh.devins.ui.compose.theme.DevInsTheme

@Composable
fun DevInsApp() {
    DevInsTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            DevInsMainContent()
        }
    }
}

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
