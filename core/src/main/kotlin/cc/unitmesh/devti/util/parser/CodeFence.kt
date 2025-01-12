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

        fun parse(content: String): CodeFence {
            val regex = Regex("```([\\w#+\\s]*)")
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
            val regex = Regex("```([\\w#+\\s]*)")
            val lines = content.replace("\\n", "\n").lines()

            var codeStarted = false
            var languageId: String? = null
            val codeBuilder = StringBuilder()
            val textBuilder = StringBuilder()

            for ((index, line) in lines.withIndex()) {
                if (!codeStarted) {
                    val matchResult = regex.find(line.trimStart())
                    if (matchResult != null) {
                        if (textBuilder.isNotEmpty()) {
                            val textBlock = CodeFence(
                                findLanguage("markdown"), textBuilder.trim().toString(), false, "txt"
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
                    if (lastTxtBlock != null && lastTxtBlock?.isComplete == false) {
                        lastTxtBlock!!.isComplete = true
                    }

                    if (line.startsWith("```")) {
                        val codeContent = codeBuilder.trim().toString()
                        val codeFence = parse("```$languageId\n$codeContent\n```")
                        codeFences.add(codeFence)

                        codeBuilder.clear()
                        codeStarted = false

                        languageId = null
                    } else {
                        codeBuilder.append(line).append("\n")
                    }
                }
            }

            val ideaLanguage = findLanguage(languageId ?: "markdown")
            if (textBuilder.isNotEmpty()) {
                val normal = CodeFence(ideaLanguage, textBuilder.trim().toString(), true, null, languageId)
                codeFences.add(normal)
            }

            if (codeStarted) {
                val codeContent = codeBuilder.trim().toString()
                if (codeContent.isNotEmpty()) {
                    val codeFence = parse("```$languageId\n$codeContent\n")
                    codeFences.add(codeFence)
                } else {
                    val defaultLanguage = CodeFence(ideaLanguage, codeContent, false, null, languageId)
                    codeFences.add(defaultLanguage)
                }
            }

            return codeFences
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