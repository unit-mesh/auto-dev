package cc.unitmesh.devins.filesystem

/**
 * JavaScript 平台的文件系统实现
 * 基于 Node.js fs 模块的高性能实现
 */
@Suppress("UNUSED_VARIABLE")
actual class DefaultFileSystem actual constructor(private val projectPath: String) : ProjectFileSystem {
    
    private val fs = js("require('fs')")
    private val path = js("require('path')")
    
    actual override fun getProjectPath(): String? = projectPath
    
    actual override fun readFile(path: String): String? {
        return try {
            val resolvedPath = resolvePathInternal(path)
            if (exists(resolvedPath) && !isDirectory(resolvedPath)) {
                val content = fs.readFileSync(resolvedPath, "utf8")
                content as String
            } else {
                null
            }
        } catch (e: Exception) {
            console.error("Error reading file: ${e.message}")
            null
        }
    }

    actual override fun writeFile(path: String, content: String): Boolean {
        return try {
            val resolvedPath = resolvePathInternal(path)
            
            // 确保父目录存在
            val dirname = this.path.dirname(resolvedPath)
            if (!exists(dirname)) {
                fs.mkdirSync(dirname, js("{ recursive: true }"))
            }
            
            fs.writeFileSync(resolvedPath, content, "utf8")
            true
        } catch (e: Exception) {
            console.error("Error writing file: ${e.message}")
            false
        }
    }

    actual override fun exists(path: String): Boolean {
        return try {
            val resolvedPath = resolvePathInternal(path)
            fs.existsSync(resolvedPath) as Boolean
        } catch (e: Exception) {
            false
        }
    }

    actual override fun isDirectory(path: String): Boolean {
        return try {
            val resolvedPath = resolvePathInternal(path)
            if (fs.existsSync(resolvedPath) as Boolean) {
                val stats = fs.statSync(resolvedPath)
                stats.isDirectory() as Boolean
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    actual override fun listFiles(path: String, pattern: String?): List<String> {
        return try {
            val dirPath = resolvePathInternal(path)
            if (!exists(dirPath) || !isDirectory(dirPath)) {
                return emptyList()
            }
            
            val files = (fs.readdirSync(dirPath) as Array<String>).toList()
            
            if (pattern != null) {
                val regexPattern = pattern
                    .replace(".", "\\.")
                    .replace("*", ".*")
                    .replace("?", ".")
                val regex = Regex(regexPattern)
                files.filter { regex.matches(it) }
            } else {
                files
            }
        } catch (e: Exception) {
            console.error("Error listing files: ${e.message}")
            emptyList()
        }
    }
    
    actual override fun searchFiles(pattern: String, maxDepth: Int, maxResults: Int): List<String> {
        return try {
            if (!exists(projectPath) || !isDirectory(projectPath)) {
                return emptyList()
            }
            
            val regexPattern = pattern
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".")
            val regex = Regex(regexPattern, RegexOption.IGNORE_CASE)
            
            val results = mutableListOf<String>()
            
            // 常见的排除目录
            val excludeDirs = setOf(
                "node_modules", ".git", ".idea", "build", "out", "target",
                "dist", ".gradle", "venv", "__pycache__", "bin", ".next",
                "coverage", ".vscode", ".DS_Store"
            )
            
            // 使用 BFS 遍历以提高性能
            searchFilesRecursive(
                projectPath,
                "",
                regex,
                excludeDirs,
                maxDepth,
                maxResults,
                results
            )
            
            results.toList()
        } catch (e: Exception) {
            console.error("Error searching files: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * 递归搜索文件
     */
    private fun searchFilesRecursive(
        basePath: String,
        relativePath: String,
        regex: Regex,
        excludeDirs: Set<String>,
        maxDepth: Int,
        maxResults: Int,
        results: MutableList<String>,
        currentDepth: Int = 0
    ) {
        if (currentDepth >= maxDepth || results.size >= maxResults) {
            return
        }
        
        try {
            val fullPath = if (relativePath.isEmpty()) {
                basePath
            } else {
                path.join(basePath, relativePath) as String
            }
            
            val entries = fs.readdirSync(fullPath) as Array<String>
            
            for (entry in entries) {
                if (results.size >= maxResults) {
                    break
                }
                
                // 跳过排除的目录
                if (entry in excludeDirs) {
                    continue
                }
                
                val entryRelativePath = if (relativePath.isEmpty()) {
                    entry
                } else {
                    path.join(relativePath, entry) as String
                }
                
                val entryFullPath = path.join(fullPath, entry) as String
                
                try {
                    val stats = fs.statSync(entryFullPath)
                    
                    if (stats.isDirectory() as Boolean) {
                        // 递归搜索子目录
                        searchFilesRecursive(
                            basePath,
                            entryRelativePath,
                            regex,
                            excludeDirs,
                            maxDepth,
                            maxResults,
                            results,
                            currentDepth + 1
                        )
                    } else if (stats.isFile() as Boolean) {
                        // 匹配文件名或完整路径
                        if (regex.matches(entry) || regex.containsMatchIn(entryRelativePath)) {
                            results.add(entryRelativePath)
                        }
                    }
                } catch (e: Exception) {
                    // 跳过无法访问的文件
                    continue
                }
            }
        } catch (e: Exception) {
            // 跳过无法访问的目录
            return
        }
    }
    
    actual override fun resolvePath(relativePath: String): String {
        return resolvePathInternal(relativePath)
    }
    
    /**
     * 解析路径为绝对路径
     */
    private fun resolvePathInternal(inputPath: String): String {
        return if (path.isAbsolute(inputPath) as Boolean) {
            path.normalize(inputPath) as String
        } else {
            path.resolve(projectPath, inputPath) as String
        }
    }
}

