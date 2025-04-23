package cc.unitmesh.devti.language.provider

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile


interface FileCreateService {
    fun createFile(prompt: String, project: Project): VirtualFile?

    companion object {
        private val languageExtension: LanguageExtension<FileCreateService> =
            LanguageExtension("cc.unitmesh.shireFileCreateService")

        fun provide(language: Language): FileCreateService? {
            return languageExtension.forLanguage(language)
        }
    }
}