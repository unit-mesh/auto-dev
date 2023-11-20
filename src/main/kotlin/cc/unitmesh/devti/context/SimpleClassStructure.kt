package cc.unitmesh.devti.context

data class SimpleClassStructure(val fieldName: String, val fieldType: String, val children: List<SimpleClassStructure>) {
    override fun toString(): String {
        return buildPumlRepresentation(this)
    }

    private fun buildPumlRepresentation(node: SimpleClassStructure, indent: String = ""): String {
        val stringBuilder = StringBuilder()

        stringBuilder.append("class ${node.fieldName} {\n")

        for (child in node.children) {
            stringBuilder.append("  ${child.fieldName}: ${child.fieldType}\n")
        }

        stringBuilder.append("}\n")

        return stringBuilder.toString()
    }
}
