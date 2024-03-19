package cc.unitmesh.devti.language.compiler.exec

import com.intellij.openapi.project.Project

class FileFuncInsCommand(val myProject: Project, val prop: String) : InsCommand {
    override fun execute(): String? {
        // a prop can be `regex("*Controller.java")`, which format is <functionName>(<arg1>, <arg2>, ...)
        // we need to parse the prop to get the function name and its arguments
        val (functionName, args) = parseRegex(prop)
        when (functionName) {
            "regex" -> {
                try {
                    val regex: Regex = Regex(args[0])
                    val allFiles = myProject.baseDir?.findFileByRelativePath("")?.children?.map { it.name } ?: emptyList()
                    val matchedFiles = allFiles.filter { regex.matches(it) }
                    return matchedFiles.joinToString(", ")
                } catch (e: Exception) {
                    return "<DevliError>: ${e.message}"
                }
            }

            else -> {
                return "<DevliError>: Unknown function: $functionName"
            }
        }
    }
}

// TODO: implement by LEX in future
fun parseRegex(prop: String): Pair<String, List<String>> {
    val regexPattern = Regex("""(\w+)\(([^)]+)\)""")
    val matchResult = regexPattern.find(prop)

    if (matchResult != null && matchResult.groupValues.size == 3) {
        val functionName = matchResult.groupValues[1]
        val args = matchResult.groupValues[2].split(',').map { it.trim() }
        return Pair(functionName, args)
    } else {
        throw IllegalArgumentException("Invalid regex pattern: $prop")
    }
}
