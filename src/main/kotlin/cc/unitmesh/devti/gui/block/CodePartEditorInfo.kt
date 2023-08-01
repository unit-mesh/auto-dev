package cc.unitmesh.devti.gui.block

import com.intellij.lang.Language
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.testFramework.LightVirtualFile
import javax.swing.JComponent

class CodePartEditorInfo(
    @JvmField val code: GraphProperty<String>,
    @JvmField val component: JComponent,
    @JvmField val editor: EditorEx,
    private val file: LightVirtualFile
) {
    var language: Language
        get() = file.language
        set(value) {
            file.setLanguage(value)
        }
}
