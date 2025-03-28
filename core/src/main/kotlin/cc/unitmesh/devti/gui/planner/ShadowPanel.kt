package cc.unitmesh.devti.gui.planner

import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

class ShadowPanel(
    private val title: String,
    private val submitButtonText: String = "Submit",
    private val cancelButtonText: String = "Cancel",
    private val onSubmit: (String) -> Unit,
    private val onCancel: () -> Unit
) : JPanel(BorderLayout()) {
    private val textArea: JTextArea
    
    init {
        border = JBUI.Borders.empty(10)
        background = JBUI.CurrentTheme.ToolWindow.background()
        
        val headerLabel = JLabel(title).apply {
            font = font.deriveFont(font.size + 2f)
            border = JBUI.Borders.emptyBottom(10)
        }
        
        textArea = JTextArea().apply {
            lineWrap = true
            wrapStyleWord = true
            border = JBUI.Borders.empty(5)
            text = ""
            rows = 15
        }
        
        val scrollPane = JBScrollPane(textArea).apply {
            preferredSize = Dimension(400, 300)
        }
        
        val buttonPanel = JPanel(BorderLayout())
        val buttonsBox = Box.createHorizontalBox().apply {
            add(JButton(submitButtonText).apply {
                addActionListener {
                    val text = textArea.text
                    if (text.isNotBlank()) {
                        onSubmit(text)
                    }
                }
            })
            add(Box.createHorizontalStrut(10))
            add(JButton(cancelButtonText).apply {
                addActionListener {
                    onCancel()
                }
            })
        }
        buttonPanel.add(buttonsBox, BorderLayout.EAST)
        buttonPanel.border = JBUI.Borders.emptyTop(10)
        
        add(headerLabel, BorderLayout.NORTH)
        add(scrollPane, BorderLayout.CENTER)
        add(buttonPanel, BorderLayout.SOUTH)
    }
    
    fun getText(): String = textArea.text
    
    fun setText(text: String) {
        textArea.text = text
    }
    
    fun requestTextAreaFocus() {
        textArea.requestFocus()
    }
}
