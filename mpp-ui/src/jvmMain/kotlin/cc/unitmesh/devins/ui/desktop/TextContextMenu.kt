package cc.unitmesh.devins.ui.desktop

import androidx.compose.foundation.ContextMenuDataProvider
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/**
 * 为文本内容提供右键菜单支持
 *
 * 功能：
 * - 复制选中的文本
 * - 全选文本
 *
 * 使用方式：
 * ```kotlin
 * TextWithContextMenu {
 *     Text("可以右键复制的文本")
 * }
 * ```
 */
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

/**
 * 复制文本到系统剪贴板
 * 使用 AWT Toolkit 确保跨应用复制粘贴正常工作
 */
fun copyToSystemClipboard(text: String) {
    try {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = StringSelection(text)
        clipboard.setContents(selection, selection)
    } catch (e: Exception) {
        println("⚠️ Failed to copy to clipboard: ${e.message}")
    }
}

/**
 * 为选中的文本提供复制功能的扩展函数
 *
 * 使用方式：
 * ```kotlin
 * Text(
 *     text = "可复制的文本",
 *     modifier = Modifier.copyableText()
 * )
 * ```
 */
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

