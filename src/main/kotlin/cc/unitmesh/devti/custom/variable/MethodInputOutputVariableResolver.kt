package cc.unitmesh.devti.custom.variable

import cc.unitmesh.devti.context.ClassContextProvider
import cc.unitmesh.devti.context.MethodContextProvider
import com.intellij.lang.LanguageCommenters
import com.intellij.psi.PsiElement

class MethodInputOutputVariableResolver(val element: PsiElement) : VariableResolver {
    override val type: CustomIntentionVariableType = CustomIntentionVariableType.METHOD_INPUT_OUTPUT
    private val commenter = LanguageCommenters.INSTANCE.forLanguage(element.language) ?: null
    val commentPrefix = commenter?.lineCommentPrefix ?: ""

    override fun resolve(): String {
        var result = ""
        val methodContext = MethodContextProvider(false, false).from(element)
        if (methodContext.name == null) {
            return ""
        }

        methodContext.inputOutputClasses.forEach {
            ClassContextProvider(false).from(it).let { classContext ->
                result += classContext.toQuery().lines().joinToString(separator = "\n") { line ->
                    "$commentPrefix  $line"
                }
            }
        }

        return result
    }
}