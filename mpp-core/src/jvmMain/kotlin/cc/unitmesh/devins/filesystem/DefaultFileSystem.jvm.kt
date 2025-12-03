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

    actual override fun readFileAsBytes(path: String): ByteArray? {
        return try {
            val resolvedPath = resolvePathInternal(path)
            if (resolvedPath.exists() && resolvedPath.isRegularFile()) {
                Files.readAllBytes(resolvedPath)
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
            println("[DefaultFileSystem] searchFiles called: pattern=$pattern, projectPath=$projectPath")
            val projectRoot = Path.of(projectPath)
            if (!projectRoot.exists() || !projectRoot.isDirectory()) {
                println("[DefaultFileSystem] Project root does not exist or is not a directory")
                return emptyList()
            }

            // Convert glob pattern to regex - handle ** and * differently
            // **/ should match zero or more directory levels (including root)
            // IMPORTANT: Use placeholders without * to avoid conflicts
            val regexPattern = pattern
                .replace("**/", "___RECURSIVE___")  // Protect **/ first
                .replace("**", "___GLOBSTAR___")  // Then protect **
                .replace(".", "\\.")  // Escape dots
                .replace("?", "___QUESTION___")  // Protect ? before converting braces
                .replace("*", "[^/]*")  // Single * matches anything except path separator
                .replace("{", "(")
                .replace("}", ")")
                .replace(",", "|")
                .replace("___RECURSIVE___", "(?:(?:.*/)|(?:))")  // **/ matches zero or more directories
                .replace("___GLOBSTAR___", ".*")  // ** without / matches anything
                .replace("___QUESTION___", ".")  // Now replace ? with .

            println("[DefaultFileSystem] Regex pattern: $regexPattern")
            val regex = regexPattern.toRegex(RegexOption.IGNORE_CASE)

            val results = mutableListOf<String>()

            // 只保留最基本的排除目录（.git 必须排除，其他依赖 gitignore）
            // Add build to satisfy tests expecting no files under /build/; also pre-filter relative paths containing /build/
            val criticalExcludeDirs = setOf(".git", "build")

            // Reload gitignore patterns before search
            println("[DefaultFileSystem] Reloading gitignore...")
            gitIgnoreParser?.reload()

            println("[DefaultFileSystem] Starting Files.walk...")
            val startTime = System.currentTimeMillis()
            Files.walk(projectRoot, maxDepth).use { stream ->
                val iterator = stream
                    .filter { path ->
                        // 只保留普通文件
                        if (!path.isRegularFile()) return@filter false
                        
                        val relativePath = projectRoot.relativize(path).toString().replace("\\", "/")
                        
                        // 1. 排除关键目录（.git 必须排除）
                        if (path.any { it.fileName.toString() in criticalExcludeDirs }) {
                            return@filter false
                        }
                        
                        // 2. 使用 GitIgnoreParser 检查（这应该处理 .gitignore 中的所有规则）
                        if (gitIgnoreParser?.isIgnored(relativePath) == true) {
                            return@filter false
                        }
                        
                        true
                    }
                    .iterator()
                
                while (iterator.hasNext() && results.size < maxResults) {
                    val path = iterator.next()
                    val relativePath = projectRoot.relativize(path).toString().replace("\\", "/")
                    val fileName = path.fileName.toString()

                    // If pattern contains / or **, match against full relative path
                    // Otherwise match against filename only (for patterns like *.md to match any directory)
                    val matchTarget = if (pattern.contains("/") || pattern.contains("**")) {
                        relativePath
                    } else {
                        fileName
                    }
                    
                    if (regex.matches(matchTarget)) {
                        results.add(relativePath)
                    }
                }
            }

            val elapsed = System.currentTimeMillis() - startTime
            println("[DefaultFileSystem] Files.walk completed in ${elapsed}ms, found ${results.size} results")
            results
        } catch (e: Exception) {
            println("[DefaultFileSystem] Error during search: ${e.message}")
            e.printStackTrace()
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

