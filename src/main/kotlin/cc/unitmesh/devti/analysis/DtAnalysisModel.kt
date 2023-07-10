package cc.unitmesh.devti.analysis

import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiClass
import com.intellij.psi.impl.source.PsiJavaFileImpl

class DtClass(
    val name: String,
    val methods: List<DtMethod>,
    val packageName: String = "",
    val path: String = ""
) {
    fun format(): String {
        val output = StringBuilder()
        output.append("class $name ")

        val constructor = methods.find { it.name == this.name }
        if (constructor != null) {
            output.append("constructor(")
            output.append(constructor.parameters.joinToString(", ") { "${it.name}: ${it.type}" })
            output.append(")\n")
        }

        if (methods.isNotEmpty()) {
            output.append("- methods: ")
            // filter out constructor
            output.append(methods.filter { it.name != this.name }.joinToString(", ") { method ->
                "${method.name}(${method.parameters.joinToString(", ") { parameter -> "${parameter.name}: ${parameter.type}" }}): ${method.returnType}"
            })
        }

        return output.toString()
    }

    companion object {
        fun Companion.fromPsiClass(psiClass: PsiClass): DtClass {
            return runReadAction {
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
                    methods = methods
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

class DtMethod(
    val name: String,
    val returnType: String,
    val parameters: List<DtParameter>
)

class DtParameter(
    val name: String,
    val type: String
)
