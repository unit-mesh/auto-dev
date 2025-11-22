package cc.unitmesh.devins.ui.desktop

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.rememberTrayState
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

/**
 * AutoDev Desktop 系统托盘
 *
 * 功能：
 * - 显示应用图标在系统托盘
 * - 提供右键菜单：显示窗口、退出
 * - 支持双击托盘图标唤醒窗口
 */
@Composable
fun ApplicationScope.AutoDevTray(
    trayState: androidx.compose.ui.window.TrayState,
    isWindowVisible: Boolean,
    onShowWindow: () -> Unit,
    onExit: () -> Unit
) {

    Tray(
        state = trayState,
        icon = loadTrayIcon(),
        tooltip = "AutoDev Desktop",
        onAction = {
            // 双击托盘图标时显示窗口
            if (!isWindowVisible) {
                onShowWindow()
            }
        },
        menu = {
            // 显示/隐藏窗口
            Item(
                text = if (isWindowVisible) "Hide Window" else "Show Window",
                onClick = {
                    if (isWindowVisible) {
                        // 当前可见，点击后隐藏（这个逻辑需要在外部处理）
                        // 这里我们只处理显示的情况
                    } else {
                        onShowWindow()
                    }
                }
            )

            Separator()

            // 退出应用
            Item(
                text = "Exit",
                onClick = onExit
            )
        }
    )
}

/**
 * 加载托盘图标
 *
 * 优先级：
 * 1. 尝试加载 resources/icon-64.png
 * 2. 如果失败，创建一个简单的默认图标
 */
private fun loadTrayIcon(): Painter {
    return try {
        // 尝试从 resources 加载图标
        val iconStream = object {}.javaClass.getResourceAsStream("/icon-64.png")
        val bufferedImage = if (iconStream != null) {
            ImageIO.read(iconStream)
        } else {
            // 如果资源不存在，创建默认图标
            createDefaultTrayIcon()
        }

        // 转换为 Compose Painter
        BitmapPainter(bufferedImage.toComposeImageBitmap())
    } catch (e: Exception) {
        println("⚠️ Failed to load tray icon: ${e.message}")
        BitmapPainter(createDefaultTrayIcon().toComposeImageBitmap())
    }
}

/**
 * 创建默认托盘图标
 * 一个简单的 64x64 蓝色圆形图标，中间有 "AD" 字样
 */
private fun createDefaultTrayIcon(): BufferedImage {
    val size = 64
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()

    try {
        // 抗锯齿
        g.setRenderingHint(
            java.awt.RenderingHints.KEY_ANTIALIASING,
            java.awt.RenderingHints.VALUE_ANTIALIAS_ON
        )

        // 绘制蓝色圆形背景
        g.color = java.awt.Color(33, 150, 243) // Material Blue
        g.fillOval(4, 4, size - 8, size - 8)

        // 绘制白色边框
        g.color = java.awt.Color.WHITE
        g.drawOval(4, 4, size - 8, size - 8)

        // 绘制 "AD" 文字
        g.color = java.awt.Color.WHITE
        g.font = java.awt.Font("Arial", java.awt.Font.BOLD, 24)
        val fm = g.fontMetrics
        val text = "AD"
        val textWidth = fm.stringWidth(text)
        val textHeight = fm.ascent
        g.drawString(
            text,
            (size - textWidth) / 2,
            (size + textHeight) / 2 - 2
        )
    } finally {
        g.dispose()
    }

    return image
}

