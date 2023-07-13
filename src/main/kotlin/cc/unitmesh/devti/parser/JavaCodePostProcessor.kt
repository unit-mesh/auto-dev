package cc.unitmesh.devti.parser

/**
 * will format complete code by prefix code and suffix code
 */
class JavaCodePostProcessor(
    private val prefixCode: String,
    private val suffixCode: String,
    private val completeCode: String,
    private val indentSize: Int = 4
) {
    private val indent = " ".repeat(indentSize)
    private val methodDeclLine = Regex("^(?:^|\\s+)(?:@[A-Z]\\w+|(?:(?:public|private|protected)\\s+)?.*\\{)")

    private val spaceRegex = Regex("\\s+")

    // todo: find a better way to format code
    fun execute(): String {
        if (completeCode.isEmpty()) {
            return completeCode
        }

        // if prefix code endsWith "    ", then it should be an indent, the complete code should also be indented
        val isNeedIndent = !prefixCode.endsWith("    ")

        var result = completeCode

        // if suffixCode includes "    }\n}", then remove completeCode's last "}\n}"
        if (result.endsWith("}\n}") and suffixCode.endsWith("}\n}")) {
            result = result.substring(0, result.length - 4)
        }

        // if prefixCode last line ends with the same space, count the space number
        val prefixLastLine = prefixCode.split("\n").last()
        val lastLineSpaceCount = spaceRegex.find(prefixLastLine)?.value?.length ?: 0

        // if suffix ends with "}", and complete code ends with "}\n}", then remove complete code's last "}"
        if (result.endsWith("}\n}") and (suffixCode.endsWith("}") || suffixCode.endsWith("}\n"))) {
            result = result.substring(0, result.length - 1)
        }

        if (isNeedIndent) {
            // if complete Code is method, not start with tab/space, add 4 spaces for each line
            if (result.startsWith("public ") || result.startsWith("private ") || result.startsWith("protected ")) {
                result = result.split("\n").joinToString("\n") { "    $it" }
            }

            // if complete code starts with annotation, then also add 4 spaces for each line
            if (result.startsWith("@")) {
                result = result.split("\n").joinToString("\n") { "    $it" }
            }
        }

        // if lastLineSpaceCount > 0, then remove the same space in a result begin if exists
        if (lastLineSpaceCount > 0) {
            val space = " ".repeat(lastLineSpaceCount)
            val spaceRegex = Regex("^\\s${space}")
            result = result.replace(spaceRegex, "")
        }

        // if result endsWith "}" then add "\n" before it
        if (result.endsWith("}")) {
            result += "\n"
        }

        return result
    }
}