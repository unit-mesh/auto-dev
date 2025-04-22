package cc.unitmesh.devti.language.ast.variable

enum class ContextVariable(
    override val variableName: String,
    override val description: String,
    override var value: Any? = "",
) :
    Variable {
    SELECTION("selection", "User selection code/element's in text"),
    SELECTION_WITH_NUM("selectionWithNum", "User selection code/element's in text with line number"),
    BEFORE_CURSOR("beforeCursor", "All the text before the cursor"),
    AFTER_CURSOR("afterCursor", "All the text after the cursor"),
    FILE_NAME("fileName", "The name of the file"),
    FILE_PATH("filePath", "The path of the file"),
    METHOD_NAME("methodName", "The name of the method"),
    LANGUAGE("language", "The language of the current file, will use IntelliJ's language ID"),
    COMMENT_SYMBOL("commentSymbol", "The comment symbol of the language, for example, `//` in Java"),
    ALL("all", "All the text")
    ;

    companion object {
        fun from(variableName: String): ContextVariable? {
            return values().find { it.variableName == variableName }
        }
    }
}
