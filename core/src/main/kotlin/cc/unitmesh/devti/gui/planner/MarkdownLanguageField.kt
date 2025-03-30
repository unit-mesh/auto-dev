package cc.unitmesh.devti.gui.planner

import com.intellij.lang.Language
import com.intellij.lang.LanguageUtil
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.colors.EditorColorsUtil
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.ui.LanguageTextField

class MarkdownLanguageField(
    private val myProject: Project?,
    val value: String,
    private val placeholder: String,
    private val fileName: String
) : LanguageTextField(
    LanguageUtil.getFileTypeLanguage(FileTypeManager.getInstance().getFileTypeByExtension("md")), myProject, value,
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
            setVerticalScrollbarVisible(false)
            setPlaceholder(placeholder)

            val scheme = EditorColorsUtil.getColorSchemeForBackground(this.colorsScheme.defaultBackground)
            this.colorsScheme = this.createBoundColorSchemeDelegate(scheme)

            settings.isLineNumbersShown = false
            settings.isLineMarkerAreaShown = false
            settings.isFoldingOutlineShown = false
            settings.isUseSoftWraps = true
        }
    }
}