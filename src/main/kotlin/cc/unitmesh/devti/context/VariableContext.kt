package cc.unitmesh.devti.context

import cc.unitmesh.devti.context.base.NamedElementContext
import com.google.gson.Gson
import com.intellij.ml.llm.context.MethodContext
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference

class VariableContext(
    override val root: PsiElement,
    override val text: String,
    override val name: String?,
    val enclosingMethod: PsiElement?,
    val enclosingClass: PsiElement?,
    val usages: List<PsiReference>,
    val includeMethodContext: Boolean,
    val includeClassContext: Boolean
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

    override fun toQuery(): String {
        return """
            var name: ${name ?: "_"}
            var method name: ${methodContext?.name ?: "_"}
            var class name: ${classContext?.name ?: "_"}
        """.trimIndent()
    }

    override fun toJson(): String = Gson().toJson(
        mapOf(
            "name" to name,
            "methodName" to (methodContext?.name),
            "className" to (classContext?.name)
        )
    )
}
