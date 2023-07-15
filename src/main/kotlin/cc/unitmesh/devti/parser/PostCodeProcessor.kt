package cc.unitmesh.devti.parser

/**
 * will format complete code by prefix code and suffix code
 */
class PostCodeProcessor(
    private val prefixCode: String,
    private val suffixCode: String,
    private val completeCode: String,
    private val indentSize: Int = 4
) {
    private val indent = " ".repeat(indentSize)
    private val methodDeclLine = Regex("^(?:^|\\s+)(?:@[A-Z]\\w+|(?:(?:public|private|protected)\\s+)?.*\\{)")

    // todo: find a better way to format code
    fun execute(): String {
        if (completeCode.isEmpty()) {
            return completeCode
        }

        var lines: MutableList<String> = completeCode.split("\n").toMutableList()
        val isFirstLineNeedIndent = !prefixCode.endsWith(indent)

        if (methodDeclLine.matches(lines[0])) {
            if (isFirstLineNeedIndent && lines[0].startsWith(indent)) {
                lines[0] = lines[0].substring(indent.length)
            }
        }

        // if lastLine not indented, indent all lines
        if (!lines.last().startsWith(indent)) {
            lines = lines.map { indent + it }.toMutableList()
        }

        val results = lines.joinToString("\n")
        val leftBraceCount = (prefixCode + completeCode + suffixCode).count { it == '{' }
        val rightBraceCount = (prefixCode + completeCode + suffixCode).count { it == '}' }

        val reversed = results.reversed()
        var toRemoveBrace = rightBraceCount - leftBraceCount
        val stringBuilder = StringBuilder()

        // Loop through the reversed string and remove unnecessary right braces
        for (i in reversed.indices) {
            if (toRemoveBrace > 0 && (reversed[i] == '}' || reversed[i] == '\n' || reversed[i] == ' ')) {
                if (reversed[i] == '}') {
                    toRemoveBrace--
                } else {
                    stringBuilder.append(reversed[i])
                }
            } else {
                stringBuilder.append(reversed[i])
            }
        }

        val output = stringBuilder.reverse().toString()

        val regex = Regex("\n\\s+\n\\s+|\n\\s+\n|\n\n\\s+")
        return regex.replace(output, "\n")
    }
}

