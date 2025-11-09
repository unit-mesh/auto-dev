package cc.unitmesh.server.service

import cc.unitmesh.server.config.ProjectsConfig
import cc.unitmesh.server.model.ProjectInfo
import java.io.File

class ProjectService(private val config: ProjectsConfig) {
    
    fun listProjects(): List<ProjectInfo> {
        val rootDir = File(config.rootPath)
        if (!rootDir.exists() || !rootDir.isDirectory) {
            return emptyList()
        }
        
        return rootDir.listFiles()
            ?.filter { it.isDirectory }
            ?.filter { isValidProject(it) }
            ?.filter { isAllowedProject(it.name) }
            ?.map { dir ->
                ProjectInfo(
                    id = dir.name,
                    name = dir.name,
                    path = dir.absolutePath,
                    description = getProjectDescription(dir)
                )
            }
            ?: emptyList()
    }
    
    fun getProject(projectId: String): ProjectInfo? {
        val projectDir = File(config.rootPath, projectId)
        if (!projectDir.exists() || !projectDir.isDirectory) {
            return null
        }
        
        if (!isAllowedProject(projectId)) {
            return null
        }
        
        return ProjectInfo(
            id = projectId,
            name = projectId,
            path = projectDir.absolutePath,
            description = getProjectDescription(projectDir)
        )
    }
    
    private fun isValidProject(dir: File): Boolean {
        // Check if it's a valid project directory
        // For MVP, just check if it has some common project files
        val indicators = listOf(
            ".git",
            "build.gradle.kts",
            "build.gradle",
            "pom.xml",
            "package.json",
            "Cargo.toml",
            "go.mod"
        )
        
        return indicators.any { File(dir, it).exists() }
    }
    
    private fun isAllowedProject(projectName: String): Boolean {
        if (config.allowedProjects.isEmpty()) {
            return true // Allow all if no restrictions
        }
        return config.allowedProjects.contains(projectName)
    }
    
    private fun getProjectDescription(dir: File): String? {
        // Try to read README.md for description
        val readmeFile = File(dir, "README.md")
        if (readmeFile.exists()) {
            val lines = readmeFile.readLines()
            // Return first non-empty line after title
            return lines.drop(1).firstOrNull { it.isNotBlank() }?.take(200)
        }
        return null
    }
}

