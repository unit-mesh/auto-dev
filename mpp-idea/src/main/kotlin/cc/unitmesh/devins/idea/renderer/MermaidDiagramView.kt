package cc.unitmesh.devins.idea.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.idea.compose.IdeaLaunchedEffect
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import com.intellij.openapi.Disposable
import com.intellij.ui.jcef.JBCefApp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.Text

/**
 * Compose wrapper for MermaidRenderer using SwingPanel.
 * Renders Mermaid diagrams using JCEF (embedded Chromium).
 *
 * @param mermaidCode The Mermaid diagram code to render
 * @param isDarkTheme Whether to use dark theme for rendering
 * @param parentDisposable Parent disposable for resource cleanup
 * @param modifier Compose modifier
 */
@Composable
fun MermaidDiagramView(
    mermaidCode: String,
    isDarkTheme: Boolean,
    parentDisposable: Disposable,
    modifier: Modifier = Modifier
) {
    // Check if JCEF is available
    if (!JBCefApp.isSupported()) {
        JcefNotAvailableView(modifier)
        return
    }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val renderer = remember {
        MermaidRenderer(parentDisposable) { success, message ->
            isLoading = false
            if (!success) {
                errorMessage = message
            }
        }
    }

    IdeaLaunchedEffect(mermaidCode, isDarkTheme) {
        isLoading = true
        errorMessage = null
        renderer.renderMermaid(mermaidCode, isDarkTheme)
    }

    Box(modifier = modifier.heightIn(min = 200.dp)) {
        SwingPanel(
            factory = { renderer.component },
            modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp)
        )

        if (isLoading) {
            LoadingOverlay()
        }

        errorMessage?.let { error ->
            ErrorOverlay(error)
        }
    }
}

@Composable
private fun JcefNotAvailableView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp)
            .background(JewelTheme.globalColors.panelBackground),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "JCEF not available - cannot render Mermaid diagrams",
            style = JewelTheme.defaultTextStyle.copy(
                fontSize = 12.sp,
                color = AutoDevColors.Amber.c500
            )
        )
    }
}

@Composable
private fun LoadingOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorOverlay(error: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(JewelTheme.globalColors.panelBackground.copy(alpha = 0.9f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Error: $error",
            style = JewelTheme.defaultTextStyle.copy(
                fontSize = 12.sp,
                color = AutoDevColors.Red.c500
            )
        )
    }
}

