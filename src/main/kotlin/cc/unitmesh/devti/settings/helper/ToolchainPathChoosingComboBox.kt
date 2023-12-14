/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package cc.unitmesh.devti.settings.helper

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.ComboBoxWithWidePopup
import com.intellij.openapi.ui.ComponentWithBrowseButton
import com.intellij.ui.ComboboxSpeedSearch
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.fields.ExtendableTextField
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.event.DocumentEvent
import javax.swing.plaf.basic.BasicComboBoxEditor

class ToolchainPathChoosingComboBox(onTextChanged: () -> Unit = {}) :
    ComponentWithBrowseButton<ComboBoxWithWidePopup<Path>>(ComboBoxWithWidePopup(), null) {
    private val editor: BasicComboBoxEditor = object : BasicComboBoxEditor() {
        override fun createEditorComponent(): ExtendableTextField = ExtendableTextField()
    }

    private val pathTextField: ExtendableTextField
        get() = childComponent.editor.editorComponent as ExtendableTextField

    init {
        ComboboxSpeedSearch(childComponent)
        childComponent.editor = editor
        childComponent.isEditable = true

        addActionListener {
            val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
            FileChooser.chooseFile(descriptor, null, null) { file ->
                childComponent.selectedItem = Paths.get(file.path)
            }
        }

        pathTextField.document.addDocumentListener(
            object : DocumentAdapter() {
                override fun textChanged(e: DocumentEvent) {
                    onTextChanged()
                }
            }
        )
    }
}
