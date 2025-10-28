package cc.unitmesh.devti.command.dataprovider

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import java.io.StringWriter
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
    
    /**
     * Compile the template with variable substitution.
     *
     * @return The compiled template string with all variables resolved
     */
    fun compile(): String {
        // 1. Parse frontmatter and content
        val (frontmatter, content) = SkillFrontmatter.parse(template)
        
        // 2. Set basic variables
        velocityContext.put("ARGUMENTS", arguments)
        
        // 3. Load and resolve variables from frontmatter
        frontmatter?.variables?.forEach { (key, value) ->
            val resolvedValue = resolveVariable(key, value)
            velocityContext.put(key, resolvedValue)
        }
        
        // 4. Add project-related variables
        addProjectVariables()
        
        // 5. Use Velocity to compile the template
        return compileWithVelocity(content)
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
    
    /**
     * Compile the template using Velocity engine
     */
    private fun compileWithVelocity(content: String): String {
        val oldContextClassLoader = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = SpecKitTemplateCompiler::class.java.classLoader
        
        val sw = StringWriter()
        try {
            Velocity.evaluate(velocityContext, sw, "#SpecKitTemplateCompiler", content)
        } catch (e: Exception) {
            logger.error("Failed to compile SpecKit template", e)
            // Return the original content if compilation fails
            sw.write(content)
        } finally {
            Thread.currentThread().contextClassLoader = oldContextClassLoader
        }
        
        return sw.toString().trim()
    }
    
    /**
     * Add a custom variable to the context
     */
    fun putVariable(key: String, value: Any) {
        velocityContext.put(key, value)
    }
    
    /**
     * Add multiple custom variables to the context
     */
    fun putAllVariables(variables: Map<String, Any>) {
        variables.forEach { (key, value) ->
            velocityContext.put(key, value)
        }
    }
}

