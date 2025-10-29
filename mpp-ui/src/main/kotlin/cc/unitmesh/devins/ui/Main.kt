package cc.unitmesh.devins.ui

import cc.unitmesh.devins.ui.swing.DevInsEditorFrame
import javax.swing.SwingUtilities
import javax.swing.UIManager

fun main() {
    SwingUtilities.invokeLater {
        DevInsEditorFrame().isVisible = true
    }
}
