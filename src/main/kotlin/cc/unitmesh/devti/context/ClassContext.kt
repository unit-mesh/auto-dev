package cc.unitmesh.devti.context

import cc.unitmesh.devti.context.base.NamedElementContext
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference

class ClassContext(
    override val root: PsiElement,
    override val text: String?,
    override val name: String?,
    val methods: List<PsiElement> = emptyList(),
    val fields: List<PsiElement> = emptyList(),
    val superClasses: List<String>? = null,
    val usages: List<PsiReference> = emptyList(),
    val displayName: String? = null
) : NamedElementContext(root, text, name) {
    private fun getFieldNames(): List<String> = fields.mapNotNull {
        VariableContextProvider(false, false, false).from(it).name
    }

    private fun getMethodSignatures(): List<String> = methods.mapNotNull {
        MethodContextProvider(false, gatherUsages = false).from(it).signature
    }

    override fun format(): String {
        val className = name ?: "_"
        val classFields = getFieldNames().joinToString(separator = "\n  ")
        val superClasses = when {
            superClasses.isNullOrEmpty() -> ""
            else -> " : ${superClasses.joinToString(separator = ", ")}"
        }
        val methodSignatures = getMethodSignatures()
            .filter { it.isNotBlank() }.joinToString(separator = "\n  ") { method ->
                "+ $method"
            }

        val filePath = displayName ?: runReadAction { root.containingFile?.virtualFile?.path }

        return """
        |'package: $filePath
        |class $className$superClasses {
        |  $classFields
        |  $methodSignatures
        |}
    """.trimMargin()
    }
}
