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
            val parser = cc.unitmesh.agent.tool.gitignore.GitIgnoreParser(projectPath)
            println("DefaultFileSystem: GitIgnoreParser 已初始化，项目路径: $projectPath")
            println("DefaultFileSystem: 加载的 gitignore 规则数: ${parser.getPatterns().size}")
            parser
        } catch (e: Exception) {
            println("DefaultFileSystem: GitIgnoreParser 初始化失败: ${e.message}")
            e.printStackTrace()
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
            
            val regexPattern = (pattern
                .replace(".", "\\.")
                .replace("**", ".*")
                .replace("*", "[^/]*") // Single * should not match path separators
                .replace("?", ".")
                .replace("{", "(")
                .replace("}", ")")
                .replace(",", "|")) + "$"  // ✓ 确保匹配到文件末尾（扩展名）
            
            val regex = regexPattern.toRegex(RegexOption.IGNORE_CASE)
            val results = mutableListOf<String>()
            
            // 只保留最基本的排除目录（.git 必须排除，其他依赖 gitignore）
            val criticalExcludeDirs = setOf(".git")
            
            // Reload gitignore patterns before search
            gitIgnoreParser?.reload()
            
            var skippedByGitignore = 0
            var skippedByExclude = 0
            
            Files.walk(projectRoot, maxDepth).use { stream ->
                val iterator = stream
                    .filter { path ->
                        // 只保留普通文件
                        if (!path.isRegularFile()) return@filter false
                        
                        val relativePath = projectRoot.relativize(path).toString().replace("\\", "/")
                        
                        // 1. 排除关键目录（.git 必须排除）
                        if (path.any { it.fileName.toString() in criticalExcludeDirs }) {
                            skippedByExclude++
                            return@filter false
                        }
                        
                        // 2. 使用 GitIgnoreParser 检查（这应该处理 .gitignore 中的所有规则）
                        if (gitIgnoreParser?.isIgnored(relativePath) == true) {
                            skippedByGitignore++
                            return@filter false
                        }
                        
                        true
                    }
                    .iterator()
                
                while (iterator.hasNext() && results.size < maxResults) {
                    val path = iterator.next()
                    val relativePath = projectRoot.relativize(path).toString().replace("\\", "/")
                    val fileName = path.fileName.toString()
                    
                    // 只匹配文件名（确保匹配的是扩展名）
                    if (regex.matches(fileName)) {
                        results.add(relativePath)
                    }
                }
            }
            
            // 输出调试信息
            if (skippedByGitignore > 0 || skippedByExclude > 0) {
                println("searchFiles: 排除 $skippedByExclude 个关键目录文件, $skippedByGitignore 个 gitignore 匹配文件")
            }
            
            results
        } catch (e: Exception) {
            println("searchFiles error: ${e.message}")
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

