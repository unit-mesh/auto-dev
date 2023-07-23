package cc.unitmesh.devti.context.model

@Deprecated("Use [MethodContextBuilder] for multiple language support")
data class DtMethod(val name: String, val returnType: String, val parameters: List<DtParameter>)