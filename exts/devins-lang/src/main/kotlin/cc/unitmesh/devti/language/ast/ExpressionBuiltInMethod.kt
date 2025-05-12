package cc.unitmesh.devti.language.ast

/**
 * This enum class `ExpressionBuiltInMethod` provides a set of built-in methods for string manipulation in Kotlin.
 * Each enum constant represents a specific built-in method, and holds information about the method's name, description,
 * the string to be inserted after the method call, and the position to move the caret to after insertion.
 *
 */
enum class ExpressionBuiltInMethod(
    val methodName: String,
    val description: String,
    val postInsertString: String = "()",
    val moveCaret: Int = 2,
) {
    LENGTH("length", "The length of the string"),
    TRIM("trim", "The trimmed string"),
    CONTAINS("contains", "Check if the string contains a substring", "(\"\")", 2),
    STARTS_WITH("startsWith", "Check if the string starts with a substring", "(\"\")", 2),
    ENDS_WITH("endsWith", "Check if the string ends with a substring", "(\"\")", 2),
    LOWERCASE("lowercase", "The lowercase version of the string"),
    UPPERCASE("uppercase", "The uppercase version of the string"),
    IS_EMPTY("isEmpty", "Check if the string is empty"),
    IS_NOT_EMPTY("isNotEmpty", "Check if the string is not empty"),
    FIRST("first", "The first character of the string"),
    LAST("last", "The last character of the string"),
    MATCHES("matches", "Check if the string matches a regex pattern", "(\"//\")", 3);

    companion object {
        fun fromString(methodName: String): ExpressionBuiltInMethod? {
            return values().find { it.methodName == methodName }
        }

        fun completionProvider(): Array<ExpressionBuiltInMethod> {
            return values()
        }
    }
}
