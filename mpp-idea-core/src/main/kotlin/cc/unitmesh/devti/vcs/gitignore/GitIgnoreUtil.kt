package cc.unitmesh.devti.vcs.gitignore

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * Utility class for gitignore operations using the new dual-engine architecture.
 * This class provides a convenient interface for checking if files should be ignored
 * and manages gitignore file loading.
 */
object GitIgnoreUtil {
    
    private val projectEngines = mutableMapOf<Project, GitIgnoreFlagWrapper>()
    
    /**
     * Checks if a file should be ignored according to gitignore rules.
     *
     * @param project the project context
     * @param file the virtual file to check
     * @return true if the file should be ignored, false otherwise
     */
    fun isIgnored(project: Project, file: VirtualFile): Boolean {
        val engine = getOrCreateEngine(project)
        val relativePath = getRelativePath(project, file) ?: return false
        return engine.isIgnored(relativePath)
    }
    
    /**
     * Checks if a file path should be ignored according to gitignore rules.
     *
     * @param project the project context
     * @param filePath the file path to check (can be relative or absolute)
     * @return true if the file should be ignored, false otherwise
     */
    fun isIgnored(project: Project, filePath: String): Boolean {
        val engine = getOrCreateEngine(project)
        val relativePath = normalizeToRelativePath(project, filePath)
        return engine.isIgnored(relativePath)
    }
    
    /**
     * Checks if a file path should be ignored according to gitignore rules.
     *
     * @param project the project context
     * @param filePath the file path to check
     * @return true if the file should be ignored, false otherwise
     */
    fun isIgnored(project: Project, filePath: Path): Boolean {
        return isIgnored(project, filePath.toString())
    }
    
    /**
     * Reloads gitignore rules for a project.
     * This will scan for .gitignore files in the project and reload the engine.
     *
     * @param project the project to reload gitignore rules for
     */
    fun reloadGitIgnore(project: Project) {
        val engine = getOrCreateEngine(project)
        val gitIgnoreContent = loadGitIgnoreContent(project)
        engine.loadFromContent(gitIgnoreContent)
    }
    
    /**
     * Gets statistics about the gitignore engine for a project.
     *
     * @param project the project
     * @return statistics map
     */
    fun getStatistics(project: Project): Map<String, Any> {
        val engine = getOrCreateEngine(project)
        return engine.getStatistics()
    }
    
    /**
     * Gets the active engine type for a project.
     *
     * @param project the project
     * @return the active engine type
     */
    fun getActiveEngineType(project: Project): IgnoreEngineFactory.EngineType {
        val engine = getOrCreateEngine(project)
        return engine.getActiveEngineType()
    }
    
    /**
     * Clears the cached engine for a project.
     * This forces a reload on the next access.
     *
     * @param project the project
     */
    fun clearCache(project: Project) {
        projectEngines.remove(project)
    }
    
    private fun getOrCreateEngine(project: Project): GitIgnoreFlagWrapper {
        return projectEngines.computeIfAbsent(project) { proj ->
            val gitIgnoreContent = loadGitIgnoreContent(proj)
            GitIgnoreFlagWrapper(proj, gitIgnoreContent)
        }
    }
    
    private fun loadGitIgnoreContent(project: Project): String {
        val projectDir = project.guessProjectDir() ?: return ""
        val gitIgnoreFile = projectDir.findChild(".gitignore") ?: return ""
        
        return try {
            gitIgnoreFile.inputStream.readBytes().toString(Charsets.UTF_8)
        } catch (e: Exception) {
            // If we can't read the .gitignore file, return empty content
            ""
        }
    }
    
    private fun getRelativePath(project: Project, file: VirtualFile): String? {
        val projectDir = project.guessProjectDir() ?: return null
        val projectPath = projectDir.toNioPath()
        val filePath = file.toNioPath()
        
        return try {
            projectPath.relativize(filePath).toString().replace('\\', '/')
        } catch (e: Exception) {
            // If we can't relativize the path, return null
            null
        }
    }
    
    private fun normalizeToRelativePath(project: Project, filePath: String): String {
        val projectDir = project.guessProjectDir()?.toNioPath() ?: return filePath
        
        val path = Path.of(filePath)
        
        return try {
            if (path.isAbsolute) {
                // Try to make it relative to project
                projectDir.relativize(path).toString().replace('\\', '/')
            } else {
                // Already relative, just normalize separators
                filePath.replace('\\', '/')
            }
        } catch (e: Exception) {
            // If relativization fails, use the original path
            filePath.replace('\\', '/')
        }
    }
}
