package cc.unitmesh.devins.command

import cc.unitmesh.devins.filesystem.ProjectFileSystem
import cc.unitmesh.yaml.YamlUtils

/**
 * SpecKit 模板编译器
 * 
 * 编译流程：
 * 1. 解析模板的 frontmatter，提取变量定义
 * 2. 解析变量值（如果是文件路径则加载文件内容）
 * 3. 使用变量替换模板中的占位符
 * 
 * 示例模板：
 * ```markdown
 * ---
 * name: Plan Skill
 * description: Execute implementation planning
 * variables:
 *   FEATURE_SPEC: "specs/001-feature/spec.md"
 *   IMPL_PLAN: "specs/001-feature/plan.md"
 * ---
 * 
 * # Implementation Plan
 * 
 * Based on the feature specification:
 * $FEATURE_SPEC
 * 
 * Generate implementation plan...
 * ```
 */
class SpecKitTemplateCompiler(
    private val fileSystem: ProjectFileSystem,
    private val template: String,
    private val command: String,
    private val input: String
) {
    private val variables = mutableMapOf<String, Any>()

    /**
     * 编译模板，返回最终输出
     */
    fun compile(): String {
        // 1. 解析 frontmatter
        val (frontmatter, content) = parseFrontmatter(template)
        
        // 2. 添加内置变量
        variables["ARGUMENTS"] = "$command $input"
        variables["COMMAND"] = command
        variables["INPUT"] = input
        
        // 3. 加载并解析 frontmatter 中定义的变量
        frontmatter?.get("variables")?.let { vars ->
            @Suppress("UNCHECKED_CAST")
            val variablesMap = vars as? Map<String, Any>
            variablesMap?.forEach { (key, value) ->
                val resolvedValue = resolveVariable(key, value)
                variables[key] = resolvedValue
            }
        }
        
        // 4. 添加项目相关变量
        addProjectVariables()
        
        // 5. 编译模板
        return compileTemplate(content)
    }
    
    /**
     * 解析 frontmatter
     * 返回 frontmatter 数据和剩余内容
     */
    private fun parseFrontmatter(markdown: String): Pair<Map<String, Any>?, String> {
        val frontmatterRegex = Regex("^---\\s*\\n([\\s\\S]*?)\\n---\\s*\\n", RegexOption.MULTILINE)
        val match = frontmatterRegex.find(markdown)

        if (match == null) {
            return Pair(null, markdown)
        }

        val yamlContent = match.groups[1]?.value ?: ""
        val endIndex = match.range.last + 1
        val contentWithoutFrontmatter = if (endIndex < markdown.length) {
            markdown.substring(endIndex)
        } else {
            ""
        }

        return try {
            val frontmatter = YamlUtils.load(yamlContent) ?: emptyMap()
            Pair(frontmatter, contentWithoutFrontmatter)
        } catch (e: Exception) {
            Pair(null, contentWithoutFrontmatter)
        }
    }
    
    /**
     * 解析变量值
     * 如果值看起来像文件路径，则加载文件内容
     */
    private fun resolveVariable(key: String, value: Any): Any {
        val valueStr = value.toString()
        
        // 检查是否为文件路径
        if (looksLikeFilePath(valueStr)) {
            val fileContent = fileSystem.readFile(valueStr)
            if (fileContent != null) {
                return fileContent
            }
        }
        
        return value
    }
    
    /**
     * 判断字符串是否看起来像文件路径
     */
    private fun looksLikeFilePath(str: String): Boolean {
        return str.contains("/") || 
               str.endsWith(".md") || 
               str.endsWith(".txt") ||
               str.endsWith(".json") ||
               str.endsWith(".yaml") ||
               str.endsWith(".yml")
    }
    
    /**
     * 添加项目相关变量
     */
    private fun addProjectVariables() {
        fileSystem.getProjectPath()?.let { path ->
            variables["PROJECT_PATH"] = path
            // 从路径中提取项目名称（最后一个目录名）
            val projectName = path.split("/", "\\").lastOrNull { it.isNotEmpty() } ?: "unknown"
            variables["PROJECT_NAME"] = projectName
        }
    }
    
    /**
     * 编译模板，替换变量占位符
     */
    private fun compileTemplate(content: String): String {
        var result = content
        variables.forEach { (key, value) ->
            result = result.replace("\$$key", value.toString())
        }
        return result.trim()
    }
    
    /**
     * 添加自定义变量
     */
    fun putVariable(key: String, value: Any) {
        variables[key] = value
    }
    
    /**
     * 批量添加变量
     */
    fun putAllVariables(vars: Map<String, Any>) {
        variables.putAll(vars)
    }
}

