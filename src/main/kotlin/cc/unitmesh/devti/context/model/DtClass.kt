package cc.unitmesh.devti.context.model

/**
 * We need to migration to [ClassContextBuilder] for multiple language support
 * [cc.unitmesh.devti.context.builder.ClassContextBuilder] is the new builder for [DtClass]
 */
class DtClass(
    val name: String,
    val methods: List<DtMethod>,
    val packageName: String = "",
    val fields: List<DtField> = listOf(),
    val path: String = ""
) {
    /**
     * Formats the given class into a documentation comment.
     *
     * The output will be in the following format:
     *
     * ```java
     * // package: cc.unitmesh.untitled.demo.service
     * // class BlogService {
     * //     blogRepository: BlogRepository
     * //     + createBlog(blogDto: CreateBlogDto): BlogPost
     * //     + getAllBlogPosts(): List<BlogPost>
     * // }
     * ```
     *
     * @return the formatted documentation comment as a string
     */
    fun commentFormat(): String {
        val output = StringBuilder()
        output.append("package: $packageName\n")
        output.append("class $name {\n")
        output.append(fields.joinToString("\n") { field ->
            "   ${field.name}: ${field.type}"
        })

        // remove getter and setter, and add them to getterSetter
        var getterSetter: List<String> = listOf()
        val methodsWithoutGetterSetter = methods
            .filter { method ->
                val isGetter = method.name.startsWith("get")
                        && method.parameters.isEmpty()
                        && !(method.name.contains("By") || method.name.contains("With") || method.name.contains("And"))

                val isSetter = method.name.startsWith("set") && method.parameters.size == 1
                if (isGetter || isSetter) {
                    getterSetter = listOf(method.name)
                    return@filter false
                }

                return@filter true
            }

        if (getterSetter.isNotEmpty()) {
            output.append("\n   'getter/setter: ${getterSetter.joinToString(", ")}\n")
        }

        val methodCodes = methodsWithoutGetterSetter
            .filter { it.name != this.name }
            .joinToString("\n") { method ->
                val params = method.parameters.joinToString("") { parameter -> "${parameter.name}: ${parameter.type}" }
                "   + ${method.name}($params)" + if (method.returnType.isNotBlank()) ": ${method.returnType}" else ""
            }

        if (methodCodes.isNotBlank()) {
            output.append("\n")
            output.append(methodCodes)
        }

        output.append("\n ' some getters and setters\n")
        output.append(" }\n")

        // TODO: split output and add comments line
        return output.split("\n").joinToString("\n") {
            "// $it"
        }
    }

    fun formatDto(): String {
        if (fields.isNotEmpty()) {
            val output = StringBuilder()
            output.append("$packageName.$name(")
            output.append(fields.joinToString(", ") { "${it.name}: ${it.type}" })
            output.append(")")
            return output.toString()
        }

        return "$packageName.$name\n"
    }

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

    /// Don't remove this, it's used by [cc.unitmesh.idea.DtModelExt] which is Kotlin compiler requirements.
    companion object
}

