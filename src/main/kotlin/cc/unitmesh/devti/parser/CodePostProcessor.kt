package cc.unitmesh.devti.parser

/**
 * will format complete code by prefix code and suffix code
 */
class CodePostProcessor(
    val prefixCode: String,
    val suffixCode: String,
    val completeCode: String
) {
    fun postProcess(): String {
        if (completeCode.isEmpty()) {
            return completeCode
        }

        var result = completeCode

        // if suffixCode includes "    }\n}", then remove completeCode's last "}\n}"
        if (result.endsWith("}\n}") and suffixCode.endsWith("}\n}")) {
            result = result.substring(0, result.length - 4)
        }

        // if complete Code is method, not start with tab/space, add 4 spaces for each line
        if (result.startsWith("public ") || result.startsWith("private ") || result.startsWith("protected ")) {
            result = result.split("\n").joinToString("\n") { "    $it" }
        }

        // if complete code starts with annotation, then also add 4 spaces for each line
        if (result.startsWith("@")) {
            result = result.split("\n").joinToString("\n") { "    $it" }
        }


        return result
    }
}