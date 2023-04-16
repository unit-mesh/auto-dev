package cc.unitmesh.devti.analysis

data class DtFile(
    val name: String,
    val className: String,
)

class DtClass(
    val name: String,
    val methods: List<DtMethod>
) {
    // output examples:
    // ```
    // class A
    // constructor(a: Int, b: String)
    // - method1(a: Int, b: String): String, method2(a: Int, b: String): String
    //```
    fun format(): String {
        val constructor = methods.find { it.name == "<init>" }
        val constructorParams = constructor?.parameters?.joinToString(", ") { "${it.name}: ${it.type}" } ?: ""
        val methodsStr = methods.filter { it.name != "<init>" }
            .joinToString(", ") { "${it.name}(${it.parameters.joinToString(", ") { "${it.name}: ${it.type}" }}): ${it.returnType}" }
        val output = "class $name\nconstructor($constructorParams)\n- $methodsStr"
        return output
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
