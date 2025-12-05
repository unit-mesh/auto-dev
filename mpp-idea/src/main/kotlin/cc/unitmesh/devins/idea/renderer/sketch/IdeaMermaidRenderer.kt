package cc.unitmesh.devins.idea.renderer.sketch

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.idea.compose.IdeaLaunchedEffect
import cc.unitmesh.devins.idea.renderer.MermaidRenderer
import cc.unitmesh.devins.idea.renderer.sketch.actions.IdeaDiagramActions
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefApp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.icons.AllIconsKeys

/**
 * Enhanced Mermaid renderer with toolbar actions.
 */
@Composable
fun IdeaMermaidRenderer(
    mermaidCode: String,
    project: Project? = null,
    isDarkTheme: Boolean = true,
    parentDisposable: Disposable,
    modifier: Modifier = Modifier
) {
    if (!JBCefApp.isSupported()) {
        JcefNotAvailableMessage(modifier)
        return
    }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }
    var showCode by remember { mutableStateOf(false) }

    val (isValid, validationError) = remember(mermaidCode) { 
        IdeaDiagramActions.validateMermaidSyntax(mermaidCode) 
    }

    val renderer = remember {
        MermaidRenderer(parentDisposable) { success, message ->
            isLoading = false
            if (!success) errorMessage = message
        }
    }

    IdeaLaunchedEffect(mermaidCode, isDarkTheme) {
        isLoading = true
        errorMessage = null
        renderer.renderMermaid(mermaidCode, isDarkTheme)
    }

    Column(modifier = modifier.background(JewelTheme.globalColors.panelBackground).clip(RoundedCornerShape(4.dp))) {
        // Toolbar
        MermaidToolbar(
            project = project,
            mermaidCode = mermaidCode,
            copied = copied,
            showCode = showCode,
            onCopy = { if (IdeaDiagramActions.copySourceToClipboard(mermaidCode)) copied = true },
            onToggleCode = { showCode = !showCode }
        )

        // Validation warning
        if (!isValid) {
            ValidationWarning(validationError)
        }

        // Code view (collapsible)
        if (showCode) {
            CodePreview(mermaidCode)
        }

        // Diagram view
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp)) {
            SwingPanel(factory = { renderer.component }, modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp))
            if (isLoading) LoadingIndicator()
            errorMessage?.let { ErrorMessage(it) }
        }
    }
}

@Composable
private fun MermaidToolbar(
    project: Project?, mermaidCode: String, copied: Boolean, showCode: Boolean,
    onCopy: () -> Unit, onToggleCode: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(JewelTheme.globalColors.panelBackground)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(AllIconsKeys.FileTypes.Diagram, "Mermaid", Modifier.size(14.dp), tint = AutoDevColors.Cyan.c400)
            Text("Mermaid", style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            DiagramActionButton(if (showCode) "Hide Code" else "Show Code",
                if (showCode) AllIconsKeys.Actions.Collapseall else AllIconsKeys.Actions.Expandall, onToggleCode)
            DiagramActionButton(if (copied) "Copied!" else "Copy",
                if (copied) AllIconsKeys.Actions.Checked else AllIconsKeys.Actions.Copy, onCopy)
        }
    }
}

@Composable
private fun DiagramActionButton(tooltip: String, iconKey: org.jetbrains.jewel.ui.icon.IconKey, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    Tooltip(tooltip = { Text(tooltip) }) {
        IconButton(onClick = onClick, modifier = Modifier.size(24.dp).hoverable(interactionSource)
            .background(if (isHovered) AutoDevColors.Neutral.c700.copy(alpha = 0.3f) else Color.Transparent)) {
            Icon(iconKey, tooltip, Modifier.size(16.dp), tint = AutoDevColors.Neutral.c300)
        }
    }
}

@Composable
private fun ValidationWarning(message: String) {
    Row(Modifier.fillMaxWidth().background(AutoDevColors.Amber.c900.copy(alpha = 0.3f)).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(AllIconsKeys.General.Warning, "Warning", Modifier.size(14.dp), tint = AutoDevColors.Amber.c400)
        Text(message, style = JewelTheme.defaultTextStyle.copy(fontSize = 11.sp, color = AutoDevColors.Amber.c300))
    }
}

@Composable
private fun CodePreview(code: String) {
    Text(code, style = JewelTheme.defaultTextStyle.copy(fontSize = 10.sp, color = AutoDevColors.Neutral.c400),
        modifier = Modifier.fillMaxWidth().background(AutoDevColors.Neutral.c900).padding(8.dp))
}

@Composable
private fun LoadingIndicator() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun ErrorMessage(error: String) {
    Box(Modifier.fillMaxWidth().background(JewelTheme.globalColors.panelBackground.copy(alpha = 0.9f)).padding(16.dp),
        contentAlignment = Alignment.Center) {
        Text("Error: $error", style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp, color = AutoDevColors.Red.c500))
    }
}

@Composable
private fun JcefNotAvailableMessage(modifier: Modifier) {
    Box(modifier.fillMaxWidth().heightIn(min = 100.dp).background(JewelTheme.globalColors.panelBackground),
        contentAlignment = Alignment.Center) {
        Text("JCEF not available", style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp, color = AutoDevColors.Amber.c500))
    }
}

