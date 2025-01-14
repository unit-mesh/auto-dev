package cc.unitmesh.devti.sketch.ui

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project


interface ExtensionLangSketch: LangSketch {
    fun getExtensionName(): String
}

interface LanguageSketchProvider {
    fun isSupported(lang: String): Boolean

    fun create(project: Project, content: String): ExtensionLangSketch

    companion object {
        private val EP_NAME: ExtensionPointName<LanguageSketchProvider> =
            ExtensionPointName("cc.unitmesh.langSketchProvider")

        fun provide(language: String): LanguageSketchProvider? {
            val lang = language.lowercase()
            return EP_NAME.extensionList.firstOrNull {
                it.isSupported(lang)
            }
        }
    }
}