package cc.unitmesh.devti.language.ast.shireql

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

/**
 * For [com.phodal.shirelang.compiler.hobbit.execute.PsiQueryStatementProcessor]
 */
interface ShireQLInterpreter {
    fun supportsMethod(language: Language, methodName: String): List<String>

    /**
     * clazz.getName() or clazz.extensions
     */
    fun resolveCall(element: PsiElement, methodName: String, arguments: List<Any>): Any

    /**
     * parentOf or childOf or anyOf ?
     */
    fun resolveOfTypedCall(project: Project, methodName: String, arguments: List<Any>): Any

    companion object {
        private val languageExtension: LanguageExtension<ShireQLInterpreter> =
            LanguageExtension("com.phodal.shirePsiQLInterpreter")

        fun provide(language: Language): ShireQLInterpreter? {
            return languageExtension.forLanguage(language)
        }
    }
}
