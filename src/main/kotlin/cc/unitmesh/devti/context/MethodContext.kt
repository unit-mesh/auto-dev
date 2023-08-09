package cc.unitmesh.devti.context

import cc.unitmesh.devti.context.base.NamedElementContext
import com.google.gson.Gson
import com.intellij.lang.LanguageCommenters
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
    val inputOutputClasses: List<PsiElement> = emptyList(),
) : NamedElementContext(
    root, text, name
) {
    private val classContext: ClassContext?
    private val commenter = LanguageCommenters.INSTANCE.forLanguage(root.language) ?: null
    private val commentPrefix = commenter?.lineCommentPrefix ?: ""
    private val project: Project = root.project


    init {
        classContext = if (includeClassContext && enclosingClass != null) {
            ClassContextProvider(false).from(enclosingClass)
        } else {
            null
        }
    }

    override fun toQuery(): String {
        val usageString = usages.joinToString("\n") {
            val classFile = it.element.containingFile
            val useText = it.element.text
            "${classFile.name} -> $useText"
        }

        var query = """language: ${language ?: "_"}
fun name: ${name ?: "_"}
fun signature: ${signature ?: "_"}
"""
        if (usageString.isNotEmpty()) {
            query += "usages: \n$usageString"
        }

        if (classContext != null) {
            query += classContext.toQuery()
        }

        return query
    }

    /**
     * convert code method to UML, like:
     * ```java
     * @GetMapping("/blog")
     * public List<BlogPost> getBlog() {
     *     return blogService.getAllBlogPosts();
     * }
     * ```
     * will be converted to:
     * ```plantuml
     * + getBlog(): List<BlogPost>
     * ```
     */
    override fun toUML(): String {
        return signature ?: ""
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

    fun inputOutputString(): String {
        var result = "```uml\n"
        this.inputOutputClasses.forEach {
            val context = ClassContextProvider(false).from(it)
            val element = context.root
            val basePath = project.basePath ?: return@forEach

            if (element.containingFile?.virtualFile?.path?.contains(basePath) != true) {
                return@forEach
            }

            context.let { classContext ->
                result += classContext.toQuery() + "\n"
            }
        }

        return "$result\n```\n"
    }
}
