package cc.unitmesh.devins.compiler.template

import cc.unitmesh.devins.compiler.variable.VariableTable

/**
 * 模板编译器
 * 将编译后的 DevIns 输出进一步处理为可执行的模板
 * 参考 @core/src/main/kotlin/cc/unitmesh/devti/custom/compile/VariableTemplateCompiler.kt
 */
class TemplateCompiler(
    private val variableTable: VariableTable = VariableTable()
) {
    
    /**
     * 编译模板字符串
     */
    fun compile(template: String): String {
        var result = template
        
        // 替换变量占位符
        result = replaceVariables(result)
        
        // 处理模板函数
        result = processTemplateFunctions(result)
        
        // 处理条件语句
        result = processConditionals(result)
        
        // 处理循环语句
        result = processLoops(result)
        
        return result
    }
    
    /**
     * 替换变量占位符
     */
    private fun replaceVariables(template: String): String {
        var result = template
        
        // 替换 ${variable} 格式的变量
        val variablePattern = Regex("""\$\{([^}]+)\}""")
        result = variablePattern.replace(result) { matchResult ->
            val variableName = matchResult.groupValues[1]
            val variableInfo = variableTable.getVariable(variableName)
            variableInfo?.value?.toString() ?: matchResult.value
        }
        
        // 替换 $variable 格式的变量
        val simpleVariablePattern = Regex("""\$([a-zA-Z_][a-zA-Z0-9_]*)""")
        result = simpleVariablePattern.replace(result) { matchResult ->
            val variableName = matchResult.groupValues[1]
            val variableInfo = variableTable.getVariable(variableName)
            variableInfo?.value?.toString() ?: matchResult.value
        }
        
        return result
    }
    
    /**
     * 处理模板函数
     */
    private fun processTemplateFunctions(template: String): String {
        var result = template
        
        // 处理 {{FUNCTION:args}} 格式的函数调用
        val functionPattern = Regex("""\{\{([A-Z_]+):([^}]*)\}\}""")
        result = functionPattern.replace(result) { matchResult ->
            val functionName = matchResult.groupValues[1]
            val arguments = matchResult.groupValues[2]
            
            when (functionName) {
                "FILE_CONTENT" -> processFileContent(arguments)
                "SYMBOL_INFO" -> processSymbolInfo(arguments)
                "WRITE_FILE" -> processWriteFile(arguments)
                "RUN_COMMAND" -> processRunCommand(arguments)
                "SHELL_EXEC" -> processShellExec(arguments)
                "SEARCH_RESULTS" -> processSearchResults(arguments)
                "APPLY_PATCH" -> processApplyPatch(arguments)
                "BROWSE_URL" -> processBrowseUrl(arguments)
                "AGENT" -> processAgent(arguments)
                "CODER_AGENT" -> processCoderAgent(arguments)
                "REVIEWER_AGENT" -> processReviewerAgent(arguments)
                "TESTER_AGENT" -> processTesterAgent(arguments)
                "ARCHITECT_AGENT" -> processArchitectAgent(arguments)
                "ANALYST_AGENT" -> processAnalystAgent(arguments)
                "WRITER_AGENT" -> processWriterAgent(arguments)
                "CUSTOM_AGENT" -> processCustomAgent(arguments)
                "CURRENT_FILE" -> getCurrentFile()
                "WORKSPACE" -> getWorkspace()
                "PROJECT_NAME" -> getProjectName()
                "LANGUAGE" -> getLanguage()
                "FRAMEWORK" -> getFramework()
                "USERNAME" -> getUsername()
                "USER_EMAIL" -> getUserEmail()
                else -> matchResult.value // 保持原样
            }
        }
        
        return result
    }
    
    /**
     * 处理条件语句
     */
    private fun processConditionals(template: String): String {
        var result = template
        
        // 简单的 #if...#end 处理
        // 使用 [\s\S] 代替 . 来匹配包括换行符的所有字符（跨平台兼容）
        val ifPattern = Regex("""#if\s*\(([^)]+)\)([\s\S]*?)#end""")
        result = ifPattern.replace(result) { matchResult ->
            val condition = matchResult.groupValues[1]
            val content = matchResult.groupValues[2]
            
            if (evaluateCondition(condition)) {
                content
            } else {
                ""
            }
        }
        
        return result
    }
    
    /**
     * 处理循环语句
     */
    private fun processLoops(template: String): String {
        var result = template
        
        // 简单的 #foreach...#end 处理
        // 使用 [\s\S] 代替 . 来匹配包括换行符的所有字符（跨平台兼容）
        val foreachPattern = Regex("""#foreach\s*\(\s*\$(\w+)\s+in\s+\$(\w+)\s*\)([\s\S]*?)#end""")
        result = foreachPattern.replace(result) { matchResult ->
            val itemVar = matchResult.groupValues[1]
            val collectionVar = matchResult.groupValues[2]
            val content = matchResult.groupValues[3]
            
            val collection = variableTable.getVariable(collectionVar)?.value
            if (collection is List<*>) {
                collection.mapIndexed { index, item ->
                    content.replace("\$$itemVar", item?.toString() ?: "")
                        .replace("\${$itemVar}", item?.toString() ?: "")
                        .replace("\$velocityCount", (index + 1).toString())
                }.joinToString("")
            } else {
                ""
            }
        }
        
        return result
    }
    
    /**
     * 评估条件表达式
     */
    private fun evaluateCondition(condition: String): Boolean {
        // 简单的条件评估
        val trimmed = condition.trim()
        
        // 检查变量是否存在且不为空
        if (trimmed.startsWith("$")) {
            val varName = trimmed.removePrefix("$")
            val variable = variableTable.getVariable(varName)
            return variable?.value != null && variable.value.toString().isNotEmpty()
        }
        
        // 检查布尔值
        return when (trimmed.lowercase()) {
            "true" -> true
            "false" -> false
            else -> false
        }
    }
    
    // 模板函数实现
    private fun processFileContent(path: String): String = "<!-- File content: $path -->"
    private fun processSymbolInfo(symbol: String): String = "<!-- Symbol info: $symbol -->"
    private fun processWriteFile(target: String): String = "<!-- Write to: $target -->"
    private fun processRunCommand(command: String): String = "<!-- Run: $command -->"
    private fun processShellExec(command: String): String = "<!-- Shell: $command -->"
    private fun processSearchResults(query: String): String = "<!-- Search: $query -->"
    private fun processApplyPatch(patch: String): String = "<!-- Patch: $patch -->"
    private fun processBrowseUrl(url: String): String = "<!-- Browse: $url -->"
    private fun processAgent(name: String): String = "<!-- Agent: $name -->"
    private fun processCoderAgent(name: String): String = "<!-- Coder Agent: $name -->"
    private fun processReviewerAgent(name: String): String = "<!-- Reviewer Agent: $name -->"
    private fun processTesterAgent(name: String): String = "<!-- Tester Agent: $name -->"
    private fun processArchitectAgent(name: String): String = "<!-- Architect Agent: $name -->"
    private fun processAnalystAgent(name: String): String = "<!-- Analyst Agent: $name -->"
    private fun processWriterAgent(name: String): String = "<!-- Writer Agent: $name -->"
    private fun processCustomAgent(name: String): String = "<!-- Custom Agent: $name -->"
    
    // 上下文函数实现
    private fun getCurrentFile(): String = "current_file.kt"
    private fun getWorkspace(): String = "/workspace"
    private fun getProjectName(): String = "project"
    private fun getLanguage(): String = "kotlin"
    private fun getFramework(): String = "spring"
    private fun getUsername(): String = "user"
    private fun getUserEmail(): String = "user@example.com"
}
