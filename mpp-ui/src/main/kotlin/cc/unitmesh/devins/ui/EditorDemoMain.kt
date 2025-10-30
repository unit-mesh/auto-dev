package cc.unitmesh.devins.ui

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cc.unitmesh.devins.ui.compose.components.DevInEditorDemo
import cc.unitmesh.devins.ui.compose.theme.DevInsTheme

/**
 * DevIn 编辑器演示应用入口
 * 独立运行编辑器组件进行测试
 * 
 * 运行方式：
 * ./gradlew :mpp-ui:runEditorDemo
 */
fun main() = application {
    val windowState = rememberWindowState(
        width = 1600.dp,
        height = 1000.dp
    )
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "DevIn Editor Demo - Testing & Development",
        state = windowState
    ) {
        DevInsTheme {
            DevInEditorDemo()
        }
    }
}

