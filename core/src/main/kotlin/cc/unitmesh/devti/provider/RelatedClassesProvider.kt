package cc.unitmesh.devti.provider

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

/**
 * The `RelatedClassesProvider` interface is used to provide related classes for a given element.
 *
 * This interface is particularly useful in languages like Java, where it can analyze elements such as
 * `PsiMethod` or `PsiField` to find related classes. The analysis includes parameters, return type, and
 * generic types of the `PsiMethod` to find related classes that are part of the project content.
 *
 * The `RelatedClassesProvider` interface also includes methods to clean up unnecessary elements in a `PsiClass`,
 * find superclasses of a `PsiClass`, and determine if an element is part of the project content.
 *
 * The main function of this interface is `lookup(element: PsiElement)`, which is used to look up related classes
 * for the given method. The function takes a `PsiElement` as an argument and returns a list of `PsiElement` objects
 * that are related to the input element.
 *
 * Note: There is a need to investigate whether this function is also needed for fields or classes.
 */
interface RelatedClassesProvider {
    /**
     * Lookup related classes for the given method.
     *
     * todo: spike is need for field or class
     */
    fun lookup(element: PsiElement): List<PsiElement>

    fun lookup(element: PsiFile): List<PsiElement>

    companion object {
        private val languageExtension: LanguageExtension<RelatedClassesProvider> =
            LanguageExtension("cc.unitmesh.relatedClassProvider")

        fun provide(language: Language): RelatedClassesProvider? {
            return languageExtension.forLanguage(language)
        }
    }
}
