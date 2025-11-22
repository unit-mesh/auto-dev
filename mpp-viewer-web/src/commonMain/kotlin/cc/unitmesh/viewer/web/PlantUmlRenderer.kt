package cc.unitmesh.viewer.web

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
expect fun PlantUmlRenderer(
    code: String,
    isDarkTheme: Boolean = true,
    modifier: Modifier = Modifier,
    onRenderComplete: ((success: Boolean, message: String) -> Unit)? = null
)
