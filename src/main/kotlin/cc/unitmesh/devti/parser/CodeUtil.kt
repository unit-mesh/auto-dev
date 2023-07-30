package cc.unitmesh.devti.parser

import com.intellij.lang.Language

class Code(val language: Language, val text: String, val isComplete: Boolean) {
    companion object {
        fun parse(content: String): Code {
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

            val trimmedCode = codeBuilder.substring(startIndex, endIndex + 1).toString()
            val language = findLanguage(languageId ?: "")
            return Code(language, trimmedCode, codeClosed)
        }

        private fun findLanguage(languageName: String): Language {
            val fixedLanguage = when (languageName) {
                "csharp" -> "c#"
                "fsharp" -> "f#"
                "cpp" -> "c++"
                else -> languageName
            }

            val languages = Language.getRegisteredLanguages()
            val registeredLanguages = languages
                .filter { it.displayName.isNotEmpty() }

            return registeredLanguages.find { it.displayName.equals(fixedLanguage, ignoreCase = true) }
                ?: Language.ANY
        }
    }
}