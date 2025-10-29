package cc.unitmesh.devti.util.parser

class MarkdownCode(val language: String, val text: String, val isComplete: Boolean = true) {
    companion object {
        fun parse(content: String): MarkdownCode {
            val regex = Regex("```([\\w#+]*)")
            val lines = content.lines()

            var codeStarted = false
            var codeClosed = false
            var languageId: String? = null
            val codeBuilder = StringBuilder()

            for (line in lines) {
                if (!codeStarted) {
                    val matchResult: MatchResult? = regex.find(line.trimStart())
                    if (matchResult != null) {
                        val substring = matchResult.groups[1]?.value
                        languageId = substring
                        codeStarted = true
                    }
                } else if (line.startsWith("```")) {
                    codeClosed = true
                    break
                } else {
                    codeBuilder.append(line).append("\n")
                }
            }

            var startIndex = 0
            var endIndex = codeBuilder.length - 1

            while (startIndex <= endIndex) {
                if (!codeBuilder[startIndex].isWhitespace()) {
                    break
                }
                startIndex++
            }

            while (endIndex >= startIndex) {
                if (!codeBuilder[endIndex].isWhitespace()) {
                    break
                }
                endIndex--
            }

            if (!codeClosed) {
                val text = codeBuilder.toString()
                if (text.isBlank()) {
                    return MarkdownCode(languageId ?: "", content, true)
                }

                return MarkdownCode(languageId ?: "", text, false)
            }

            val trimmedCode = codeBuilder.substring(startIndex, endIndex + 1).toString()
            return MarkdownCode(languageId ?: "", trimmedCode)
        }
    }
}