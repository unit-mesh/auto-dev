package cc.unitmesh.devti.util.parser

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.PlainTextLanguage

class Code(val language: Language, val text: String, val isComplete: Boolean) {
    companion object {
        fun parse(content: String): Code {
            val regex = Regex("```([\\w#+]*)")
            // convert content \\n to \n
            val lines = content.replace("\\n", "\n").lines()

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

            // if content is not empty, but code is empty, then it's a markdown
            if (trimmedCode.isEmpty()) {
                return Code(findLanguage("markdown"), content.replace("\\n", "\n"), codeClosed)
            }

            return Code(language, trimmedCode, codeClosed)
        }

        fun findLanguage(languageName: String): Language {
            val fixedLanguage = when (languageName) {
                "csharp" -> "c#"
                "cpp" -> "c++"
                else -> languageName
            }

            val languages = Language.getRegisteredLanguages()
            val registeredLanguages = languages
                .filter { it.displayName.isNotEmpty() }

            return registeredLanguages.find { it.displayName.equals(fixedLanguage, ignoreCase = true) }
                ?: PlainTextLanguage.INSTANCE
        }
    }
}