package cc.unitmesh.devins.filesystem

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.streams.toList

/**
 * JVM 平台的文件系统实现
 */
actual class DefaultFileSystem actual constructor(private val projectPath: String) : ProjectFileSystem {
    
    private val gitIgnoreParser: cc.unitmesh.agent.tool.gitignore.GitIgnoreParser? by lazy {
        try {
            cc.unitmesh.agent.tool.gitignore.GitIgnoreParser(projectPath)
        } catch (e: Exception) {
            null
        }
    }

    actual override fun getProjectPath(): String? = projectPath
    
    actual override fun readFile(path: String): String? {
        return try {
            val resolvedPath = resolvePathInternal(path)
            if (resolvedPath.exists() && resolvedPath.isRegularFile()) {
                resolvedPath.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    actual override fun writeFile(path: String, content: String): Boolean {
        return try {
            val resolvedPath = resolvePathInternal(path)
            // 确保父目录存在
            resolvedPath.parent?.let { parent ->
                if (!parent.exists()) {
                    Files.createDirectories(parent)
                }
            }
            resolvedPath.writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }

    actual override fun exists(path: String): Boolean {
        return try {
            resolvePathInternal(path).exists()
        } catch (e: Exception) {
            false
        }
    }

    actual override fun isDirectory(path: String): Boolean {
        return try {
            val resolvedPath = resolvePathInternal(path)
            resolvedPath.exists() && resolvedPath.isDirectory()
        } catch (e: Exception) {
            false
        }
    }
    
    actual override fun listFiles(path: String, pattern: String?): List<String> {
        return try {
            val dirPath = resolvePathInternal(path)
            if (!dirPath.exists() || !dirPath.isDirectory()) {
                return emptyList()
            }
            
            val files = Files.list(dirPath).use { stream ->
                stream.filter { it.isRegularFile() }
                    .map { it.fileName.toString() }
                    .toList()
            }
            
            if (pattern != null) {
                val regex = pattern.replace("*", ".*").replace("?", ".").toRegex()
                files.filter { regex.matches(it) }
            } else {
                files
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    actual override fun searchFiles(pattern: String, maxDepth: Int, maxResults: Int): List<String> {
        return try {
            val projectRoot = Path.of(projectPath)
            if (!projectRoot.exists() || !projectRoot.isDirectory()) {
                return emptyList()
            }
            
            val regexPattern = pattern
                .replace(".", "\\.")
                .replace("**", ".*")
                .replace("*", "[^/]*") // Single * should not match path separators
                .replace("?", ".")
                .replace("{", "(")
                .replace("}", ")")
                .replace(",", "|")
            
            val regex = regexPattern.toRegex(RegexOption.IGNORE_CASE)
            val results = mutableListOf<String>()
            
            // 常见的排除目录
            val excludeDirs = setOf(
                "node_modules", ".git", ".idea", "build", "out", "target", 
                "dist", ".gradle", "venv", "__pycache__", "bin"
            )
            
            // Reload gitignore patterns before search
            gitIgnoreParser?.reload()
            
            Files.walk(projectRoot, maxDepth).use { stream ->
                stream.filter { path ->
                    // 只保留普通文件
                    if (!path.isRegularFile()) return@filter false
                    
                    // 排除在排除目录中的文件
                    if (path.any { it.fileName.toString() in excludeDirs }) return@filter false
                    
                    // Check gitignore
                    val relativePath = projectRoot.relativize(path).toString()
                    if (gitIgnoreParser?.isIgnored(relativePath) == true) return@filter false
                    
                    true
                }
                .limit(maxResults.toLong())
                .forEach { path ->
                    val relativePath = projectRoot.relativize(path).toString()
                    val fileName = path.fileName.toString()
                    
                    // 匹配文件名或完整路径
                    if (regex.matches(fileName) || regex.containsMatchIn(relativePath)) {
                        results.add(relativePath)
                    }
                }
            }
            
            results
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    actual override fun resolvePath(relativePath: String): String {
        return resolvePathInternal(relativePath).toString()
    }

    actual override fun createDirectory(path: String): Boolean {
        return try {
            val resolvedPath = resolvePathInternal(path)
            if (!resolvedPath.exists()) {
                Files.createDirectories(resolvedPath)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun resolvePathInternal(path: String): Path {
        val p = Path.of(path)
        return if (p.isAbsolute) {
            p
        } else {
            Path.of(projectPath, path)
        }
    }
}

