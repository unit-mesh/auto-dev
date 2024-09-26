package cc.unitmesh.devti.provider

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

/**
 * The `CustomPromptProvider` interface is an extension point for providing variables support for LLMs' custom prompt template.
 * Usecases:
 * - [Team Prompts](https://ide.unitmesh.cc/custom/team-prompts), a new ways to collaborate and share your brilliance with your team.
 * - [Custom Prompt](https://ide.unitmesh.cc/custom/action), a new ways to customize your own prompt template.
 */
interface CustomPromptProvider {

    /**
     * Retrieves the code snippet of the test method with the specified name.
     */
    fun underTestMethodCode(project: Project, element: PsiElement): List<String>

    companion object {
        private val languageExtension: LanguageExtension<CustomPromptProvider> =
            LanguageExtension("cc.unitmesh.customPromptProvider")

        fun forLanguage(language: Language): CustomPromptProvider? {
            return languageExtension.forLanguage(language)
        }
    }
}
