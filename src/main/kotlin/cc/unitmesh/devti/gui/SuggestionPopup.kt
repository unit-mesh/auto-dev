package cc.unitmesh.devti.gui

import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.EditorTextField
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JPanel

fun createSuggestionPopup(suggestion: String): JBPopup {
    val myTextField = EditorTextField(suggestion)
    myTextField.size = Dimension(640, 480)

    val panel = JPanel(BorderLayout(0, 20))
    panel.add(myTextField, BorderLayout.CENTER)

    val builder = JBPopupFactory.getInstance().createComponentPopupBuilder(panel, myTextField)

    val popup = builder.createPopup()
    popup.setMinimumSize(Dimension(320, 240))
    popup.size = Dimension(640, 480)

    return popup
}