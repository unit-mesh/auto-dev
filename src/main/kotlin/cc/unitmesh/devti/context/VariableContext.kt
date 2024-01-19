package cc.unitmesh.devti.context

import cc.unitmesh.devti.context.base.NamedElementContext
import com.google.gson.Gson
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference

class VariableContext(
    override val root: PsiElement,
    override val text: String,
    override val name: String?,
    val enclosingMethod: PsiElement? = null,
    val enclosingClass: PsiElement? = null,
    val usages: List<PsiReference> = emptyList(),
    val includeMethodContext: Boolean = false,
    val includeClassContext: Boolean = false
) : NamedElementContext(
    root, text, name
) {

    private val methodContext: MethodContext?
    private val classContext: ClassContext?

    init {
        methodContext = if (includeMethodContext && enclosingMethod != null) {
            MethodContextProvider(false, false).from(enclosingMethod)
        } else {
            null
        }
        classContext = if (includeClassContext && enclosingClass != null) {
            ClassContextProvider(false).from(enclosingClass)
        } else {
            null
        }
    }

    override fun format(): String {
        return """
            'variable -> ${root.text}
        """.trimIndent()
    }
}
