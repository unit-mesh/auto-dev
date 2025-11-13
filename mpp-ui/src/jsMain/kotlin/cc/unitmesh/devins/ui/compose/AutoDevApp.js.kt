package cc.unitmesh.devins.ui.compose

import androidx.compose.runtime.Composable

/**
 * JS 专属的 AutoDevApp 实现
 * 
 * 委托给 commonMain 中的 AutoDevApp 实现
 */
@Composable
actual fun PlatformAutoDevApp(
    triggerFileChooser: Boolean,
    onFileChooserHandled: () -> Unit,
    initialMode: String
) {
    // JS 平台使用 commonMain 中的原有实现
    AutoDevApp(
        triggerFileChooser = triggerFileChooser,
        onFileChooserHandled = onFileChooserHandled,
        initialMode = initialMode
    )
}

