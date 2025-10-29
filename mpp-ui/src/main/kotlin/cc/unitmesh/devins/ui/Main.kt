package cc.unitmesh.devins.ui

import cc.unitmesh.devins.ui.swing.DevInsEditorFrame
import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.extras.FlatSVGIcon
import javax.swing.SwingUtilities
import javax.swing.UIManager

fun main() {
    // 设置 FlatLaf 暗色主题
    try {
        FlatDarkLaf.setup()

        // 设置一些自定义属性
        UIManager.put("Tree.showDefaultIcons", true)
        UIManager.put("Component.focusWidth", 1)
        UIManager.put("ScrollBar.showButtons", false)
        UIManager.put("ScrollBar.width", 12)

    } catch (e: Exception) {
        e.printStackTrace()
    }

    SwingUtilities.invokeLater {
        DevInsEditorFrame().isVisible = true
    }
}
