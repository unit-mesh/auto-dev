package cc.unitmesh.devti.devins.variable

enum class ConditionPsiVariable(
    override val variableName: String,
    override val description: String,
    override var value: Any? = null,
) : Variable {
    FILE_PATH("filePath", "The path of the file"),
    FILE_NAME("fileName", "The name of the file"),
    FILE_EXTENSION("fileExtension", "The extension of the file"),
    FILE_CONTENT("fileContent", "The content of the file")
    ;

    companion object {
        fun from(variableName: String): ConditionPsiVariable? {
            return values().find { it.variableName == variableName }
        }
    }
}