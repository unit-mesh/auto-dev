package cc.unitmesh.devti.gui.chat.view

import cc.unitmesh.devti.agent.view.WebBlockView
import com.intellij.temporary.gui.block.CodeBlockView
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JPanel

class FrontendCodeView(webview: WebBlockView, codeView: CodeBlockView) : JBPanel<MessageView>() {
    init {
        isDoubleBuffered = true
        isOpaque = true
        background = JBColor(0xE0EEF7, 0x2d2f30)

        layout = BorderLayout(JBUI.scale(8), 0)
        val centerPanel = JPanel(VerticalLayout(JBUI.scale(8)))

        centerPanel.isOpaque = false
        centerPanel.border = JBUI.Borders.emptyRight(8)
        centerPanel.background = JBColor(0xE0EEF7, 0x2d2f30)

        add(centerPanel, BorderLayout.WEST)

        webview.initialize()
        centerPanel.add(webview.getComponent())

        codeView.initialize()
        centerPanel.add(codeView.getComponent())
    }
}