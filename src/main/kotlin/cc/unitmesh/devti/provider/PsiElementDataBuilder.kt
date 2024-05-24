package cc.unitmesh.devti.provider

import cc.unitmesh.devti.context.ClassContext
import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

/**
 * The `TestDataBuilder` interface provides methods for generating test data for a given Kotlin language class.
 *
 * It defines the following methods:
 * - `baseRoute(element: PsiElement): String`: This method is used to lookup the base route of the method for the parent element.
 *   It takes a `PsiElement` as a parameter and returns a `String` representing the base route. If no base route is found, an empty string is returned.
 *
 * - `inboundData(element: PsiElement): Map<String, String>`: This method is used to generate inbound test data for the given element.
 *   It takes a `PsiElement` as a parameter and returns a `Map<String, String>` representing the inbound test data. The keys in the map represent the data field names, and the values represent the corresponding data values.
 *   If no inbound data is found, an empty map is returned.
 *
 * - `outboundData(element: PsiElement): Map<String, String>`: This method is used to generate outbound test data for the given element.
 *   It takes a `PsiElement` as a parameter and returns a `Map<String, String>` representing the outbound test data. The keys in the map represent the data field names, and the values represent the corresponding data values.
 *   If no outbound data is found, an empty map is returned.
 *
 * The `TestDataBuilder` interface also provides a companion object with the following method:
 * - `forLanguage(language: Language): TestDataBuilder?`: This method is used to retrieve a `TestDataBuilder` instance for the specified language.
 *   It takes a `Language` as a parameter and returns a `TestDataBuilder` instance associated with the given language. If no instance is found, null is returned.
 *
 * Note: The `TestDataBuilder` interface is intended to be implemented by concrete classes that provide the actual implementation for generating test data.
 *
 * @see PsiElement
 * @see Language
 * @see LanguageExtension
 */
interface PsiElementDataBuilder {
    /**
     * Lookup the base route of the Method for parent
     */
    fun baseRoute(element: PsiElement): String = ""

    fun inboundData(element: PsiElement): Map<String, String> = mapOf()

    fun outboundData(element: PsiElement): Map<String, String> = mapOf()

    fun lookupElement(project: Project, canonicalName: String): ClassContext? = null

    companion object {
        private val languageExtension: LanguageExtension<PsiElementDataBuilder> =
            LanguageExtension("cc.unitmesh.testDataBuilder")

        fun forLanguage(language: Language): PsiElementDataBuilder? {
            return languageExtension.forLanguage(language)
        }
    }
}