package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.foundation.ContextMenuDataProvider
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cc.unitmesh.devins.ui.desktop.copyToSystemClipboard

/**
 * JVM 平台实现 - 支持右键菜单复制
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
actual fun PlatformMessageTextContainer(
    text: String,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    ContextMenuDataProvider(
        items = {
            listOf(
                ContextMenuItem("Copy") {
                    copyToSystemClipboard(text)
                }
            )
        }
    ) {
        SelectionContainer(modifier = modifier) {
            content()
        }
    }
}

