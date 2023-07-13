package cc.unitmesh.devti.parser

/**
 * will format complete code by prefix code and suffix code
 */
class JavaCodePostProcessor(
    private val prefixCode: String,
    private val suffixCode: String,
    private val completeCode: String
) {
    private val spaceRegex = Regex("\\s+")

    fun execute(): String {
        if (completeCode.isEmpty()) {
            return completeCode
        }

        var result = completeCode

        // if suffixCode includes "    }\n}", then remove completeCode's last "}\n}"
        if (result.endsWith("}\n}") and suffixCode.endsWith("}\n}")) {
            result = result.substring(0, result.length - 4)
        }

        // if prefixCode last line ends with same space, count the space number
        val prefixLastLine = prefixCode.split("\n").last()
        val lastLineSpaceCount = spaceRegex.find(prefixLastLine)?.value?.length ?: 0

        // if complete Code is method, not start with tab/space, add 4 spaces for each line
        if (result.startsWith("public ") || result.startsWith("private ") || result.startsWith("protected ")) {
            result = result.split("\n").joinToString("\n") { "    $it" }
        }

        // if complete code starts with annotation, then also add 4 spaces for each line
        if (result.startsWith("@")) {
            result = result.split("\n").joinToString("\n") { "    $it" }
        }

        // if suffix ends with "}", and complete code ends with "}\n}", then remove complete code's last "}"
        if (result.endsWith("}\n}") and suffixCode.endsWith("}")) {
            result = result.substring(0, result.length - 1)
        }

        // if lastLineSpaceCount > 0, then remove same space in result begin if exists
        if (lastLineSpaceCount > 0) {
            val spaceRegex = Regex("^\\s{$lastLineSpaceCount}")
            result = result.replace(spaceRegex, "")
        }

        return result
    }
}