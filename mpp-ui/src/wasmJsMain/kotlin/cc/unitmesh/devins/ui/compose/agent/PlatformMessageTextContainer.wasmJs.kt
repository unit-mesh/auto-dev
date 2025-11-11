package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * JS 平台实现 - 支持文本选择
 */
@Composable
actual fun PlatformMessageTextContainer(
    text: String,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    SelectionContainer(modifier = modifier) {
        content()
    }
}

