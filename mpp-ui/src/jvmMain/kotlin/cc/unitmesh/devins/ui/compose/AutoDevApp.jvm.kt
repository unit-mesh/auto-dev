package cc.unitmesh.devins.ui.compose

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformAutoDevApp(
    triggerFileChooser: Boolean,
    onFileChooserHandled: () -> Unit,
    initialMode: String
) {
    AutoDevApp(
        triggerFileChooser = triggerFileChooser,
        onFileChooserHandled = onFileChooserHandled,
        initialMode = initialMode
    )
}

