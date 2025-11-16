package cc.unitmesh.devins.ui.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope

/**
 * Desktop Window Layout
 *
 * 标准的桌面应用窗口布局，包含：
 * - 自定义标题栏（可拖拽）
 * - 窗口控制按钮（最小化、最大化、关闭）
 * - 圆角边框
 * - 主内容区域
 *
 * 注意：需要配合 Window(undecorated = true) 使用
 */
@Composable
fun FrameWindowScope.DesktopWindowLayout(
    title: String = "AutoDev",
    showWindowControls: Boolean = true,
    onMinimize: () -> Unit = {},
    onMaximize: () -> Unit = {},
    onClose: () -> Unit = {},
    titleBarContent: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit
) {
    // 整个窗口的圆角和阴影
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ),
        shadowElevation = 8.dp,
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            WindowDraggableArea {
                TitleBar(
                    title = title,
                    showWindowControls = showWindowControls,
                    onMinimize = onMinimize,
                    onMaximize = onMaximize,
                    onClose = onClose,
                    content = titleBarContent
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                content()
            }
        }
    }
}

/**
 * 自定义标题栏
 *
 * 布局：[窗口控制] [标题/内容]
 */
@Composable
private fun TitleBar(
    title: String,
    showWindowControls: Boolean,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onClose: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(32.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
//            if (showWindowControls) {
                WindowControls(
                    onMinimize = onMinimize,
                    onMaximize = onMaximize,
                    onClose = onClose
                )

                Spacer(modifier = Modifier.width(12.dp))
//            }

            content()
        }
    }
}

