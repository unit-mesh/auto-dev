package cc.unitmesh.devti.provider.local

import com.intellij.lang.Language
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.colors.EditorColorsUtil
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.ui.LanguageTextField
import java.awt.Dimension
import java.awt.FontMetrics

interface JsonTextProvider {
    fun createComponent(myProject: Project?, value: String, placeholder: String, fileName: String): LanguageTextField

    companion object {
        private val EP_NAME: ExtensionPointName<JsonTextProvider> =
            ExtensionPointName("cc.unitmesh.jsonTextProvider")

        fun create(myProject: Project?, value: String, placeholder: String, fileName: String): LanguageTextField {
            return EP_NAME.extensionList.map {
                it.createComponent(myProject, value, placeholder, fileName)
            }.firstOrNull() ?: DefaultLanguageField(myProject, value, placeholder, fileName)
        }
    }
}

class DefaultLanguageField(
    private val myProject: Project?,
    val value: String,
    private val placeholder: String,
    private val fileName: String
) :
    LanguageTextField(PlainTextLanguage.INSTANCE, myProject, value,
        object : SimpleDocumentCreator() {
            override fun createDocument(value: String?, language: Language?, project: Project?): Document {
                return createDocument(value, language, project, this)
            }

            override fun customizePsiFile(file: PsiFile?) {
                file?.name = fileName
            }
        }
    ) {

    override fun createEditor(): EditorEx {
        return super.createEditor().apply {
            setShowPlaceholderWhenFocused(true)
            setHorizontalScrollbarVisible(false)
            setVerticalScrollbarVisible(true)
            setPlaceholder(placeholder)

            val scheme = EditorColorsUtil.getColorSchemeForBackground(this.colorsScheme.defaultBackground)
            this.colorsScheme = this.createBoundColorSchemeDelegate(scheme)
            isOneLineMode = false
        }
    }
}
