package cc.unitmesh.devti.analysis

import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiClass
import com.intellij.psi.impl.source.PsiJavaFileImpl

data class DtMethod(val name: String, val returnType: String, val parameters: List<DtParameter>)
data class DtField(val name: String, val type: String)
data class DtParameter(val name: String, val type: String)

class DtClass(
    val name: String,
    val methods: List<DtMethod>,
    val packageName: String = "",
    val fields: List<DtField> = listOf(),
    val path: String = ""
) {
    /**
     * Output:
     * ```
     * // package: cc.unitmesh.untitled.demo.service
     * // class BlogService {
     * // blogRepository: BlogRepository
     * //  + createBlog(blogDto: CreateBlogDto): BlogPost
     * //  + getAllBlogPosts(): List<BlogPost>
     * //}
     * ```
     */
    fun format(): String {
        val output = StringBuilder()
        output.append("// package: $packageName\n")
        output.append("// class $name {\n")
        output.append(fields.joinToString("\n") { field ->
            "//   ${field.name}: ${field.type}"
        })

        // remove getter and setter, and add them to getterSetter
        var getterSetter: List<String> = listOf()
        val methodsWithoutGetterSetter = methods
            .filter { method ->
                val isGetter = method.name.startsWith("get") && method.parameters.isEmpty()
                val isSetter = method.name.startsWith("set") && method.parameters.size == 1
                if (isGetter || isSetter) {
                    getterSetter = listOf(method.name)
                    return@filter false
                }

                return@filter true
            }

        if (getterSetter.isNotEmpty()) {
            output.append("\n//   'getter/setter: ${getterSetter.joinToString(", ")}\n")
        }

        val methodCodes = methodsWithoutGetterSetter
            .filter { it.name != this.name }
            .joinToString("\n") { method ->
                val params = method.parameters.joinToString("") { parameter -> "${parameter.name}: ${parameter.type}" }
                "//   + ${method.name}($params)" + if (method.returnType.isNotBlank()) ": ${method.returnType}" else ""
            }

        if (methodCodes.isNotBlank()) {
            output.append("\n")
            output.append(methodCodes)
        }

        output.append("\n// ' some getters and setters\n")
        output.append("// }\n")

        return output.toString()
    }

    companion object {
        fun Companion.fromPsiClass(psiClass: PsiClass): DtClass {
            return runReadAction {
                val fields = psiClass.fields.map { field ->
                    DtField(
                        name = field.name,
                        type = field.type.toString().replace("PsiType:", "")
                    )
                }

                val methods = psiClass.methods.map { method ->
                    // if method is getter or setter, skip
                    if (method.name.startsWith("get") || method.name.startsWith("set")) {
                        return@map null
                    }

                    DtMethod(
                        name = method.name,
                        returnType = method.returnType?.presentableText ?: "",
                        parameters = method.parameters.map { parameter ->
                            DtParameter(
                                name = parameter.name ?: "",
                                type = parameter.type.toString().replace("PsiType:", "")
                            )
                        }
                    )
                }.filterNotNull()

                return@runReadAction DtClass(
                    packageName = psiClass.qualifiedName ?: "",
                    path = psiClass.containingFile?.virtualFile?.path ?: "",
                    name = psiClass.name ?: "",
                    methods = methods,
                    fields = fields
                )
            }
        }

        fun fromJavaFile(file: PsiJavaFileImpl?): DtClass {
            return runReadAction {
                val psiClass = file?.classes?.firstOrNull() ?: return@runReadAction DtClass("", emptyList())
                return@runReadAction fromPsiClass(psiClass)
            }
        }
    }
}
