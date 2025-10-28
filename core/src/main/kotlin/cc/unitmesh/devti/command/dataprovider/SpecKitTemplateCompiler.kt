package cc.unitmesh.devti.command.dataprovider

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.apache.velocity.VelocityContext
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * Template compiler for SpecKit commands, similar to VariableTemplateCompiler.
 *
 * This compiler:
 * 1. Parses frontmatter from the template to extract variable definitions
 * 2. Resolves variables by loading file contents or using provided values
 * 3. Uses Velocity template engine to compile the final output
 *
 * Example usage:
 * ```kotlin
 * val compiler = SpecKitTemplateCompiler(project, template, arguments)
 * val result = compiler.compile()
 * ```
 */
class SpecKitTemplateCompiler(
    private val project: Project,
    private val template: String,
    private val arguments: String
) {
    private val logger = logger<SpecKitTemplateCompiler>()
    private val velocityContext = VelocityContext()

    companion object {
        /**
         * Check if Velocity engine should be used.
         * In test mode, always use simple replacement to avoid Velocity initialization issues.
         */
        private fun shouldUseVelocity(): Boolean {
            // In unit test mode, always use simple replacement
            if (ApplicationManager.getApplication()?.isUnitTestMode == true) {
                return false
            }

            // In production, use Velocity
            return true
        }
    }
    
    /**
     * Compile the template with variable substitution.
     *
     * @return The compiled template string with all variables resolved
     */
    fun compile(): String {
        val (frontmatter, content) = SkillFrontmatter.parse(template)
        
        velocityContext.put("ARGUMENTS", arguments)
        
        // 3. Load and resolve variables from frontmatter
        frontmatter?.variables?.forEach { (key, value) ->
            val resolvedValue = resolveVariable(key, value)
            velocityContext.put(key, resolvedValue)
        }
        
        addProjectVariables()
        
        return templateCompile(content)
    }
    
    /**
     * Resolve a variable value. If the value looks like a file path, load the file content.
     * Otherwise, return the value as-is.
     */
    private fun resolveVariable(key: String, value: Any): Any {
        val valueStr = value.toString()
        
        // Check if this looks like a file path
        if (valueStr.contains("/") || valueStr.endsWith(".md") || valueStr.endsWith(".txt")) {
            return loadFileContent(valueStr) ?: valueStr
        }
        
        return value
    }
    
    /**
     * Load content from a file path. Supports both absolute and relative paths.
     * Returns null if the file doesn't exist or can't be read.
     */
    private fun loadFileContent(filePath: String): String? {
        try {
            val projectPath = project.basePath ?: return null
            
            // Try as absolute path first
            var path = Path.of(filePath)
            if (!path.exists()) {
                // Try as relative path from project root
                path = Path.of(projectPath, filePath)
            }
            
            if (!path.exists()) {
                logger.warn("File not found: $filePath")
                return null
            }
            
            return path.readText()
        } catch (e: Exception) {
            logger.warn("Failed to load file: $filePath", e)
            return null
        }
    }
    
    /**
     * Add project-related variables to the context
     */
    private fun addProjectVariables() {
        project.basePath?.let { basePath ->
            velocityContext.put("PROJECT_PATH", basePath)
            velocityContext.put("PROJECT_NAME", project.name)
        }
    }
    
    private fun templateCompile(content: String): String {
        var result = content
        velocityContext.keys.forEach { key ->
            val value = velocityContext.get(key.toString())
            if (value != null) {
                result = result.replace("\$$key", value.toString())
            }
        }

        return result.trim()
    }

    fun putVariable(key: String, value: Any) {
        velocityContext.put(key, value)
    }

    fun putAllVariables(variables: Map<String, Any>) {
        variables.forEach { (key, value) ->
            velocityContext.put(key, value)
        }
    }
}

