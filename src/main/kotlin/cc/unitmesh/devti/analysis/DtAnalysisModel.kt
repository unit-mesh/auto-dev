package cc.unitmesh.devti.analysis

data class DtFile(
    val name: String,
    val className: String,
)

class DtClass(
    val name: String,
    val methods: List<DtMethod>
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

    companion object
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
