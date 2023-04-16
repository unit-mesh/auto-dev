package cc.unitmesh.devti.analysis

class DtClass(
    val name: String,
    val methods: List<DtMethod>
)

class DtMethod(
    val name: String,
    val returnType: String,
    val parameters: List<DtParameter>
)

class DtParameter(
    val name: String,
    val type: String
)
