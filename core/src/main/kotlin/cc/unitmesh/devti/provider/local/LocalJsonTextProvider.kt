package cc.unitmesh.devti.provider.local

import com.intellij.lang.Language
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.colors.EditorColorsUtil
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.ui.LanguageTextField
import java.awt.Dimension
import java.awt.FontMetrics

class LocalJsonTextProvider : JsonTextProvider {
    override fun createComponent(
        myProject: Project?,
        value: String,
        placeholder: String,
        fileName: String
    ): LanguageTextField {
        return JsonLanguageField(myProject, value, placeholder, fileName)
    }
}

class JsonLanguageField(
    private val myProject: Project?,
    val value: String,
    private val placeholder: String = "",
    private val fileName: String? = null,
    private val oneLineMode: Boolean = false
) :
    LanguageTextField(
        Language.findLanguageByID("JSON"), myProject, value,
        object : SimpleDocumentCreator() {
            override fun createDocument(value: String?, language: Language?, project: Project?): Document {
                return createDocument(value, language, project, this)
            }

//            override fun customizePsiFile(file: PsiFile?) {
//                if (fileName != null) {
//                    file?.name = fileName
//                }
//            }
        }
    ) {

    override fun createEditor(): EditorEx {
        return super.createEditor().apply {
            setShowPlaceholderWhenFocused(true)
//            setHorizontalScrollbarVisible(false)
//            setVerticalScrollbarVisible(true)
            setPlaceholder(placeholder)

            val scheme = EditorColorsUtil.getColorSchemeForBackground(this.colorsScheme.defaultBackground)
            this.colorsScheme = this.createBoundColorSchemeDelegate(scheme)

            this.settings.isUseSoftWraps = true
//            this.settings.isAdditionalPageAtBottom = false
//            this.settings.isCaretRowShown = false

            isOneLineMode = oneLineMode
        }
    }
}
