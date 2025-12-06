package cc.unitmesh.devins.ui.nano

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/**
 * Preview entry point for NanoDSL Demo
 *
 * Run this file directly to preview the NanoDSL renderer.
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "NanoDSL Demo",
        state = rememberWindowState(width = 1200.dp, height = 800.dp)
    ) {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                NanoDSLDemo(
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Preview
@Composable
fun NanoDSLDemoPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            NanoDSLDemo(modifier = Modifier.padding(8.dp))
        }
    }
}

