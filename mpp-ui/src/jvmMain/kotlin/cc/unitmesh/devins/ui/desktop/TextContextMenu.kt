package cc.unitmesh.devins.ui.desktop

import androidx.compose.foundation.ContextMenuDataProvider
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TextWithContextMenu(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    ContextMenuDataProvider(
        items = {
            listOf(
                ContextMenuItem("Copy") {
                    copyToSystemClipboard("")
                }
            )
        }
    ) {
        SelectionContainer(modifier = modifier) {
            content()
        }
    }
}

fun copyToSystemClipboard(text: String) {
    try {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = StringSelection(text)
        clipboard.setContents(selection, selection)
    } catch (e: Exception) {
        println("⚠️ Failed to copy to clipboard: ${e.message}")
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.copyableText(text: String): Modifier {
    return this.then(
        Modifier
    )
}

/**
 * 简化的文本复制容器
 * 自动为内部文本添加选择和复制功能
 */
@Composable
fun CopyableTextContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    SelectionContainer(modifier = modifier) {
        content()
    }
}

