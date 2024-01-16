package cc.unitmesh.devti.context

import cc.unitmesh.devti.context.base.NamedElementContext
import cc.unitmesh.devti.util.isInProject
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference

class MethodContext(
    override val root: PsiElement,
    override val text: String,
    override val name: String?,
    val signature: String? = null,
    val enclosingClass: PsiElement? = null,
    val language: String? = null,
    val returnType: String? = null,
    val paramNames: List<String> = emptyList(),
    val includeClassContext: Boolean = false,
    val usages: List<PsiReference> = emptyList(),
    private val fanInOut: List<PsiElement> = emptyList(),
) : NamedElementContext(root, text, name) {
    private val classContext: ClassContext?
    private val project: Project = root.project

    init {
        classContext = if (includeClassContext && enclosingClass != null) {
            ClassContextProvider(false).from(enclosingClass)
        } else {
            null
        }
    }

    override fun format(): String {
        val usageString = usages.joinToString("\n") {
            val classFile = it.element.containingFile
            val useText = it.element.text
            "${classFile.name} -> $useText"
        }

        var query = """
            path: ${root.containingFile?.virtualFile?.path ?: "_"}
            language: ${language ?: "_"}
            fun name: ${name ?: "_"}
            fun signature: ${signature ?: "_"}
            """.trimIndent()

        if (usageString.isNotEmpty()) {
            query += "\nusages: \n$usageString"
        }

        if (classContext != null) {
            query += classContext.format()
        }

        return query
    }

    fun inputOutputString(): String {
        if (fanInOut.isEmpty()) return ""

        var result = ""
        this.fanInOut.forEach {
            val context: ClassContext = ClassContextProvider(false).from(it)
            val element = context.root

            if (!isInProject(element.containingFile?.virtualFile!!, project)) {
                return@forEach
            }

            context.let { classContext ->
                result += "${classContext.format()}\n"
            }
        }

        if (result.isEmpty()) {
            return ""
        }

        return """
            ```uml
            $result
            ```
            """.trimIndent()
    }
}
