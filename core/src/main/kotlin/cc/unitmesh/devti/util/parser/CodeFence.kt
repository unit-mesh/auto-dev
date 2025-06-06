package cc.unitmesh.devti.util.parser

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.PlainTextLanguage

class CodeFence(
    val language: Language,
    val text: String,
    var isComplete: Boolean,
    val extension: String? = null,
    val originLanguage: String? = null
) {
    companion object {
        private var lastTxtBlock: CodeFence? = null
        val devinStartRegex = Regex("<devin>")
        val devinEndRegex = Regex("</devin>")

        fun parse(content: String): CodeFence {
            val languageRegex = Regex("\\s*```([\\w#+ ]*)")
            val lines = content.lines()

            val startMatch = devinStartRegex.find(content)
            if (startMatch != null) {
                val endMatch = devinEndRegex.find(content)
                val isComplete = endMatch != null

                val devinContent = if (isComplete) {
                    content.substring(startMatch.range.last + 1, endMatch!!.range.first).trim()
                } else {
                    content.substring(startMatch.range.last + 1).trim()
                }

                return CodeFence(findLanguage("DevIn"), devinContent, isComplete, "devin", "DevIn")
            }

            var codeStarted = false
            var codeClosed = false
            var languageId: String? = null
            var codeIndentation = ""
            val codeBuilder = StringBuilder()

            for (line in lines) {
                if (!codeStarted) {
                    val trimmedLine = line.trimStart()
                    val matchResult: MatchResult? = languageRegex.find(trimmedLine)
                    if (matchResult != null) {
                        // Store the indentation to match it when looking for the closing fence
                        codeIndentation = line.substring(0, line.length - trimmedLine.length)
                        val substring = matchResult.groups[1]?.value?.trim()
                        languageId = substring
                        codeStarted = true
                    }
                } else {
                    val trimmedLine = line.trimStart()
                    if (trimmedLine == "```") {
                        codeClosed = true
                        break
                    } else {
                        codeBuilder.append(line).append("\n")
                    }
                }
            }

            val trimmedCode = codeBuilder.trim().toString()
            val language = findLanguage(languageId ?: "")
            val extension = language.associatedFileType?.defaultExtension
                ?: lookupFileExt(languageId ?: "txt")

            return if (trimmedCode.isEmpty()) {
                CodeFence(language, "", codeClosed, extension, languageId)
            } else {
                CodeFence(language, trimmedCode, codeClosed, extension, languageId)
            }
        }

        fun parseAll(content: String): List<CodeFence> {
            val codeFences = mutableListOf<CodeFence>()
            var currentIndex = 0
            var content = content
            if (content.contains("```devin\n")) {
                content = preProcessDevinBlock(content)
            }

            val startMatches = devinStartRegex.findAll(content)
            for (startMatch in startMatches) {
                if (startMatch.range.first > currentIndex) {
                    val beforeText = content.substring(currentIndex, startMatch.range.first)
                    if (beforeText.trim().isNotEmpty()) {
                        parseMarkdownContent(beforeText, codeFences)
                    }
                }

                // 在整个内容中查找对应的结束标签，而不是在子字符串中查找
                val endMatch = devinEndRegex.find(content, startMatch.range.last + 1)
                val isComplete = endMatch != null

                val devinContent = if (isComplete) {
                    content.substring(startMatch.range.last + 1, endMatch!!.range.first).trim()
                } else {
                    content.substring(startMatch.range.last + 1).trim()
                }

                codeFences.add(CodeFence(findLanguage("DevIn"), devinContent, isComplete, "devin", "DevIn"))
                currentIndex = if (isComplete) {
                    endMatch!!.range.last + 1
                } else {
                    content.length
                }
            }

            if (currentIndex < content.length) {
                val remainingContent = content.substring(currentIndex)
                if (remainingContent.trim().isNotEmpty()) {
                    parseMarkdownContent(remainingContent, codeFences)
                }
            }

            return codeFences.filter {
                if (it.originLanguage == "DevIn") {
                    return@filter true
                }

                return@filter it.text.isNotEmpty()
            }
        }

        val devinRegexBlock = Regex("(?<=^|\\n)```devin\\n([\\s\\S]*?)\\n```\\n")
        val normalCodeBlock = Regex("\\s*```([\\w#+ ]*)\\n")

        fun preProcessDevinBlock(content: String): String {
            var currentContent = content

            val devinMatches = devinRegexBlock.findAll(content).toList()

            for (match in devinMatches) {
                var devinContent = match.groups[1]?.value ?: ""
                if (normalCodeBlock.find(devinContent) != null) {
                    if (!devinContent.trim().endsWith("```")) {
                        devinContent += "\n```"
                    }
                }

                val replacement = "\n<devin>\n$devinContent\n</devin>"
                currentContent = currentContent.replace(match.value, replacement)
            }

            return currentContent
        }

        private fun parseMarkdownContent(content: String, codeFences: MutableList<CodeFence>) {
            val languageRegex = Regex("\\s*```([\\w#+ ]*)")
            val lines = content.lines()

            var codeStarted = false
            var languageId: String? = null
            val codeBuilder = StringBuilder()
            val textBuilder = StringBuilder()
            var codeIndentation = ""

            for (i in lines.indices) {
                val line = lines[i]
                if (!codeStarted) {
                    // Check for code block start with any indentation
                    val trimmedLine = line.trimStart()
                    val matchResult = languageRegex.find(trimmedLine)
                    if (matchResult != null) {
                        if (textBuilder.isNotEmpty()) {
                            val textBlock = CodeFence(
                                findLanguage("markdown"), textBuilder.trim().toString(), true, "md"
                            )
                            lastTxtBlock = textBlock
                            codeFences.add(textBlock)
                            textBuilder.clear()
                        }

                        // Store the indentation to match it when looking for the closing fence
                        codeIndentation = line.substring(0, line.length - trimmedLine.length)
                        languageId = matchResult.groups[1]?.value?.trim()
                        codeStarted = true
                    } else {
                        textBuilder.append(line).append("\n")
                    }
                } else {
                    // Check if this line contains the closing fence with the same or similar indentation
                    val trimmedLine = line.trimStart()

                    // Allow for some flexibility in indentation for the closing fence
                    // This helps with numbered lists where indentation might vary slightly
                    if (trimmedLine == "```") {
                        val codeContent = codeBuilder.trim().toString()
                        val codeFence = CodeFence(
                            findLanguage(languageId ?: "markdown"),
                            codeContent,
                            true,
                            lookupFileExt(languageId ?: "md"),
                            languageId
                        )
                        codeFences.add(codeFence)

                        codeBuilder.clear()
                        codeStarted = false
                        languageId = null
                        codeIndentation = ""
                    } else {
                        codeBuilder.append(line).append("\n")
                    }
                }
            }

            if (textBuilder.isNotEmpty()) {
                val textBlock = CodeFence(findLanguage("markdown"), textBuilder.trim().toString(), true, "md")
                codeFences.add(textBlock)
            }

            if (codeStarted && codeBuilder.isNotEmpty()) {
                val code = codeBuilder.trim().toString()
                val codeFence = CodeFence(
                    findLanguage(languageId ?: "markdown"), code, false, lookupFileExt(languageId ?: "md"), languageId
                )
                codeFences.add(codeFence)
            }
        }

        val languages = Language.getRegisteredLanguages()
        val registeredLanguages = languages.filter { it.displayName.isNotEmpty() }

        /**
         * Searches for a language by its name and returns the corresponding [Language] object. If the language is not found,
         * [PlainTextLanguage.INSTANCE] is returned.
         *
         * @param languageName The name of the language to find.
         * @return The [Language] object corresponding to the given name, or [PlainTextLanguage.INSTANCE] if the language is not found.
         */
        fun findLanguage(languageName: String): Language {
            val fixedLanguage = when (languageName) {
                "csharp" -> "c#"
                "cpp" -> "c++"
                "shell" -> "Shell Script"
                "sh" -> "Shell Script"
                "bash" -> "Shell Script"
                "http" -> "HTTP Request"
                "plan" -> "Plain Text"
                "go" -> "Go"
                else -> languageName
            }

            return registeredLanguages.find { it.displayName.equals(fixedLanguage, ignoreCase = true) }
                ?: PlainTextLanguage.INSTANCE
        }

        fun findLanguageByExt(extension: String): Language? {
            languages.forEach {
                if (it.associatedFileType?.defaultExtension == extension) {
                    return it
                }
            }

            return null
        }

        fun lookupFileExt(languageId: String): String {
            return when (languageId.lowercase()) {
                "c#" -> "cs"
                "c++" -> "cpp"
                "c" -> "c"
                "java" -> "java"
                "javascript" -> "js"
                "kotlin" -> "kt"
                "python" -> "py"
                "ruby" -> "rb"
                "swift" -> "swift"
                "typescript" -> "ts"
                "markdown" -> "md"
                "sql" -> "sql"
                "plantuml" -> "puml"
                "shell" -> "sh"
                "objective-c" -> "m"
                "objective-c++" -> "mm"
                "go" -> "go"
                "html" -> "html"
                "css" -> "css"
                "dart" -> "dart"
                "scala" -> "scala"
                "rust" -> "rs"
                "http request" -> "http"
                "shell script" -> "sh"
                "bash" -> "sh"
                "devin" -> "devin"
                else -> languageId
            }
        }

        fun displayNameByExt(extension: String): String {
            return when (extension.lowercase()) {
                "cs" -> "C#"
                "cpp" -> "C++"
                "c" -> "C"
                "java" -> "Java"
                "js" -> "JavaScript"
                "kt" -> "Kotlin"
                "py" -> "Python"
                "rb" -> "Ruby"
                "swift" -> "Swift"
                "ts" -> "TypeScript"
                "md" -> "Markdown"
                "sql" -> "SQL"
                "puml" -> "PlantUML"
                "sh" -> "Shell Script"
                "m" -> "Objective-C"
                "mm" -> "Objective-C++"
                "go" -> "Go"
                "html" -> "HTML"
                "css" -> "CSS"
                "dart" -> "Dart"
                "scala" -> "Scala"
                "rs" -> "Rust"
                "http" -> "HTTP Request"
                else -> extension
            }
        }
    }
}

private fun IntRange.length(): Int {
    return (this.endInclusive - this.start) + 1
}

