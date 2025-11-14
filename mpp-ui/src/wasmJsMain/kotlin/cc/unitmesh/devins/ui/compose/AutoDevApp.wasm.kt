package cc.unitmesh.devins.ui.compose

import androidx.compose.runtime.Composable

/**
 * WASM 专属的 AutoDevApp 实现
 *
 * 委托给 commonMain 中的 AutoDevApp 实现
 */
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

