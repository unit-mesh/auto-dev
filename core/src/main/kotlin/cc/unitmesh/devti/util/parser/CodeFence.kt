package cc.unitmesh.devti.util.parser

import ai.grazie.nlp.utils.length
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
            val markdownRegex = Regex("```([\\w#+\\s]*)")

            val lines = content.lines()

            // 检查是否存在 devin 开始标签
            val startMatch = devinStartRegex.find(content)
            if (startMatch != null) {
                val endMatch = devinEndRegex.find(content)
                val isComplete = endMatch != null

                // 提取内容：如果有结束标签就截取中间内容，没有就取整个后续内容
                val devinContent = if (isComplete) {
                    content.substring(startMatch.range.last + 1, endMatch!!.range.first).trim()
                } else {
                    content.substring(startMatch.range.last + 1).trim()
                }

                return CodeFence(findLanguage("DevIn"), devinContent, isComplete, "devin", "DevIn")
            }

            // 原有的 Markdown 代码块解析逻辑
            var codeStarted = false
            var codeClosed = false
            var languageId: String? = null
            val codeBuilder = StringBuilder()

            for (line in lines) {
                if (!codeStarted) {
                    val matchResult: MatchResult? = markdownRegex.find(line.trimStart())
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

            val trimmedCode = codeBuilder.trim().toString()
            val language = findLanguage(languageId ?: "")
            val extension =
                language.associatedFileType?.defaultExtension ?: lookupFileExt(languageId ?: "txt")

            return if (trimmedCode.isEmpty()) {
                CodeFence(language, "", codeClosed, extension, languageId)
            } else {
                CodeFence(language, trimmedCode, codeClosed, extension, languageId)
            }
        }

        fun parseAll(content: String): List<CodeFence> {
            val codeFences = mutableListOf<CodeFence>()
            var currentIndex = 0

            val startMatches = devinStartRegex.findAll(content)
            for (startMatch in startMatches) {
                // 处理标签前的文本
                if (startMatch.range.first > currentIndex) {
                    val beforeText = content.substring(currentIndex, startMatch.range.first)
                    if (beforeText.isNotEmpty()) {
                        parseMarkdownContent(beforeText, codeFences)
                    }
                }

                // 处理 devin 标签内容
                val searchRegion = content.substring(startMatch.range.first)
                val endMatch = devinEndRegex.find(searchRegion)
                val isComplete = endMatch != null

                val devinContent = if (isComplete) {
                    searchRegion.substring(startMatch.range.length, endMatch!!.range.first).trim()
                } else {
                    searchRegion.substring(startMatch.range.length).trim()
                }

                codeFences.add(CodeFence(findLanguage("DevIn"), devinContent, isComplete, "devin", "DevIn"))
                currentIndex = if (isComplete) {
                    startMatch.range.first + endMatch!!.range.last + 1
                } else {
                    content.length
                }
            }

            // 处理最后剩余的内容
            if (currentIndex < content.length) {
                val remainingContent = content.substring(currentIndex)
                parseMarkdownContent(remainingContent, codeFences)
            }

            return codeFences
        }

        private fun parseMarkdownContent(content: String, codeFences: MutableList<CodeFence>) {
            val regex = Regex("```([\\w#+\\s]*)")
            val lines = content.lines()

            var codeStarted = false
            var languageId: String? = null
            val codeBuilder = StringBuilder()
            val textBuilder = StringBuilder()

            for (line in lines) {
                if (!codeStarted) {
                    val matchResult = regex.find(line.trimStart())
                    if (matchResult != null) {
                        if (textBuilder.isNotEmpty()) {
                            val textBlock = CodeFence(
                                findLanguage("markdown"), textBuilder.trim().toString(), true, "txt"
                            )
                            lastTxtBlock = textBlock
                            codeFences.add(textBlock)
                            textBuilder.clear()
                        }

                        languageId = matchResult.groups[1]?.value
                        codeStarted = true
                    } else {
                        textBuilder.append(line).append("\n")
                    }
                } else {
                    if (line.startsWith("```")) {
                        val codeContent = codeBuilder.trim().toString()
                        val codeFence = CodeFence(
                            findLanguage(languageId ?: ""),
                            codeContent,
                            true,
                            lookupFileExt(languageId ?: "txt"),
                            languageId
                        )
                        codeFences.add(codeFence)

                        codeBuilder.clear()
                        codeStarted = false
                        languageId = null
                    } else {
                        codeBuilder.append(line).append("\n")
                    }
                }
            }

            // 处理最后的文本内容
            if (textBuilder.isNotEmpty()) {
                val textBlock = CodeFence(
                    findLanguage("markdown"), 
                    textBuilder.trim().toString(), 
                    true, 
                    "txt"
                )
                codeFences.add(textBlock)
            }

            // 处理未闭合的代码块
            if (codeStarted && codeBuilder.isNotEmpty()) {
                val codeFence = CodeFence(
                    findLanguage(languageId ?: ""),
                    codeBuilder.trim().toString(),
                    false,
                    lookupFileExt(languageId ?: "txt"),
                    languageId
                )
                codeFences.add(codeFence)
            }
        }

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
                else -> languageName
            }

            val languages = Language.getRegisteredLanguages()
            val registeredLanguages = languages.filter { it.displayName.isNotEmpty() }

            return registeredLanguages.find { it.displayName.equals(fixedLanguage, ignoreCase = true) }
                ?: PlainTextLanguage.INSTANCE
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