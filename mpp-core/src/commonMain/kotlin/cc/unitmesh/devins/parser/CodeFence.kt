package cc.unitmesh.devins.parser

/**
 * 代码围栏解析器 - 核心解析逻辑
 * 从 IDEA 插件版本移植，去除 IntelliJ Platform 依赖
 */
class CodeFence(
    val languageId: String,
    val text: String,
    var isComplete: Boolean,
    val extension: String? = null
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

                return CodeFence("devin", devinContent, isComplete, "devin")
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
            val extension = lookupFileExt(languageId ?: "txt")

            return if (trimmedCode.isEmpty()) {
                CodeFence(languageId ?: "", "", codeClosed, extension)
            } else {
                CodeFence(languageId ?: "", trimmedCode, codeClosed, extension)
            }
        }

        /**
         * 解析所有代码块（包括文本块和代码块）
         */
        fun parseAll(content: String): List<CodeFence> {
            val codeFences = mutableListOf<CodeFence>()
            var currentIndex = 0
            var processedContent = content
            
            if (content.contains("```devin\n")) {
                processedContent = preProcessDevinBlock(content)
            }

            val startMatches = devinStartRegex.findAll(processedContent)
            for (startMatch in startMatches) {
                if (startMatch.range.first > currentIndex) {
                    val beforeText = processedContent.substring(currentIndex, startMatch.range.first)
                    if (beforeText.trim().isNotEmpty()) {
                        parseMarkdownContent(beforeText, codeFences)
                    }
                }

                // 在整个内容中查找对应的结束标签
                val endMatch = devinEndRegex.find(processedContent, startMatch.range.last + 1)
                val isComplete = endMatch != null

                val devinContent = if (isComplete) {
                    processedContent.substring(startMatch.range.last + 1, endMatch!!.range.first).trim()
                } else {
                    processedContent.substring(startMatch.range.last + 1).trim()
                }

                codeFences.add(CodeFence("devin", devinContent, isComplete, "devin"))
                currentIndex = if (isComplete) {
                    endMatch!!.range.last + 1
                } else {
                    processedContent.length
                }
            }

            if (currentIndex < processedContent.length) {
                val remainingContent = processedContent.substring(currentIndex)
                if (remainingContent.trim().isNotEmpty()) {
                    parseMarkdownContent(remainingContent, codeFences)
                }
            }

            return codeFences.filter {
                if (it.languageId == "devin") {
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
                                "markdown", textBuilder.trim().toString(), true, "md"
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
                    // Check if this line contains the closing fence
                    val trimmedLine = line.trimStart()

                    if (trimmedLine == "```") {
                        val codeContent = codeBuilder.trim().toString()
                        val codeFence = CodeFence(
                            languageId ?: "markdown",
                            codeContent,
                            true,
                            lookupFileExt(languageId ?: "md")
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
                val textBlock = CodeFence("markdown", textBuilder.trim().toString(), true, "md")
                codeFences.add(textBlock)
            }

            if (codeStarted && codeBuilder.isNotEmpty()) {
                val code = codeBuilder.trim().toString()
                val codeFence = CodeFence(
                    languageId ?: "markdown", code, false, lookupFileExt(languageId ?: "md")
                )
                codeFences.add(codeFence)
            }
        }

        /**
         * 根据语言 ID 查找文件扩展名
         */
        fun lookupFileExt(languageId: String): String {
            return when (languageId.lowercase()) {
                "c#", "csharp" -> "cs"
                "c++", "cpp" -> "cpp"
                "c" -> "c"
                "java" -> "java"
                "javascript", "js" -> "js"
                "kotlin", "kt" -> "kt"
                "python", "py" -> "py"
                "ruby", "rb" -> "rb"
                "swift" -> "swift"
                "typescript", "ts" -> "ts"
                "markdown", "md" -> "md"
                "sql" -> "sql"
                "plantuml", "puml" -> "puml"
                "shell", "bash", "sh" -> "sh"
                "objective-c" -> "m"
                "objective-c++" -> "mm"
                "go" -> "go"
                "html" -> "html"
                "css" -> "css"
                "dart" -> "dart"
                "scala" -> "scala"
                "rust", "rs" -> "rs"
                "http request", "http" -> "http"
                "shell script" -> "sh"
                "devin" -> "devin"
                "json" -> "json"
                "yaml", "yml" -> "yaml"
                "xml" -> "xml"
                "toml" -> "toml"
                "dockerfile" -> "dockerfile"
                else -> languageId
            }
        }

        /**
         * 根据文件扩展名获取显示名称
         */
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
                "json" -> "JSON"
                "yaml" -> "YAML"
                "xml" -> "XML"
                "toml" -> "TOML"
                else -> extension.uppercase()
            }
        }
    }
}

