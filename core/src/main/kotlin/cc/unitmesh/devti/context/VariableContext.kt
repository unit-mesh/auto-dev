package cc.unitmesh.devti.context

import cc.unitmesh.devti.context.base.NamedElementContext
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference

class VariableContext(
    override val root: PsiElement,
    override val text: String,
    override val name: String?,
    val enclosingMethod: PsiElement? = null,
    val enclosingClass: PsiElement?= null,
    val usages: List<PsiReference> = emptyList(),
    val includeMethodContext: Boolean = false,
    val includeClassContext: Boolean = false
) : NamedElementContext(
    root, text, name
) {
    private val methodContext: MethodContext? = if (includeMethodContext && enclosingMethod != null) {
        MethodContextProvider(false, false).from(enclosingMethod)
    } else {
        null
    }

    private val classContext: ClassContext? = if (includeClassContext && enclosingClass != null) {
        ClassContextProvider(false).from(enclosingClass)
    } else {
        null
    }

    fun shortFormat(): String = runReadAction {  root.text ?: ""}

    /**
     * Returns a formatted string representation of the method.
     *
     * The returned string includes the following information:
     * - The name of the method, or "_" if the name is null.
     * - The name of the method's context, or "_" if the context is null.
     * - The name of the class's context, or "_" if the context is null.
     *
     * @return A formatted string representation of the method.
     */
    override fun format(): String {
        return """
            var name: ${name ?: "_"}
            var method name: ${methodContext?.name ?: "_"}
            var class name: ${classContext?.name ?: "_"}
        """.trimIndent()
    }
}
