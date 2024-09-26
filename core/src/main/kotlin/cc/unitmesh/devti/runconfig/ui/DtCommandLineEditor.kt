package cc.unitmesh.devti.runconfig.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.TextAccessor
import com.intellij.util.TextFieldCompletionProvider
import com.intellij.util.textCompletion.TextFieldWithCompletion
import java.awt.BorderLayout
import javax.swing.JPanel

class DtCommandLineEditor(private val project: Project, private val completionProvider: TextFieldCompletionProvider) :
    JPanel(BorderLayout()), TextAccessor {
    private val textField = createTextField("")

    init {
        add(textField, BorderLayout.CENTER)
    }

    override fun setText(text: String?) {
        textField.setText(text)
    }

    override fun getText(): String = textField.text

    private fun createTextField(value: String): TextFieldWithCompletion =
        TextFieldWithCompletion(
            project,
            completionProvider,
            value,
            true,
            false,
            false
        )
}
