package cc.unitmesh.devti.context

import cc.unitmesh.devti.context.base.NamedElementContext
import com.google.gson.Gson
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference

class MethodContext(
    override val root: PsiElement,
    override val text: String,
    override val name: String?,
    val signature: String? = null,
    val enclosingClass: PsiElement?= null,
    val language: String?= null,
    val returnType: String?= null,
    val paramNames: List<String> = emptyList(),
    val includeClassContext: Boolean = false,
    val usages: List<PsiReference> = emptyList()
) : NamedElementContext(
    root, text, name
) {

    private val classContext: ClassContext?

    init {
        classContext = if (includeClassContext && enclosingClass != null) {
            ClassContextProvider(false).from(enclosingClass)
        } else {
            null
        }
    }

    override fun toQuery(): String {
        val query = """
        fun name: ${name ?: "_"}
        fun language: ${language ?: "_"}
        fun signature: ${signature ?: "_"}
        fun code: $text
    """.trimIndent()

        if (classContext != null) {
            return query + classContext.toQuery()
        }

        return query
    }

    override fun toJson(): String = Gson().toJson(
        mapOf(
            "text" to text,
            "name" to name,
            "signature" to signature,
            "returnType" to returnType,
            "paramNames" to paramNames,
            "language" to language,
            "class" to classContext?.toJson()
        )
    )
}
