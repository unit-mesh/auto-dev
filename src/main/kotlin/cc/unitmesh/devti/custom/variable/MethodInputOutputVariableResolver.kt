package cc.unitmesh.devti.custom.variable

import cc.unitmesh.devti.context.ClassContextProvider
import cc.unitmesh.devti.context.MethodContextProvider
import com.intellij.lang.LanguageCommenters
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

class MethodInputOutputVariableResolver(val element: PsiElement) : VariableResolver {
    override val type: CustomIntentionVariableType = CustomIntentionVariableType.METHOD_INPUT_OUTPUT
    private val commenter = LanguageCommenters.INSTANCE.forLanguage(element.language) ?: null
    private val commentPrefix = commenter?.lineCommentPrefix ?: ""
    private val project: Project = element.project

    override fun resolve(): String {
        var result = ""
        val methodContext = MethodContextProvider(false, false).from(element)
        if (methodContext.name == null) {
            return ""
        }

        methodContext.inputOutputClasses.forEach {
            val context = ClassContextProvider(false).from(it)
            val element = context.root
            val basePath = project.basePath ?: return@forEach

            if (element.containingFile?.virtualFile?.path?.contains(basePath) != true) {
                return@forEach
            }

            context.let { classContext ->
                result += classContext.toQuery().lines().joinToString(separator = "\n") { line ->
                    "$commentPrefix $line"
                }
                result += "\n"
            }
        }

        return result
    }
}