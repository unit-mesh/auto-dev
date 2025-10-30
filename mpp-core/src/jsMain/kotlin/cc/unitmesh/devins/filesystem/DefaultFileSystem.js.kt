package cc.unitmesh.devins.filesystem

/**
 * JavaScript 平台的文件系统实现
 * 目前提供空实现，未来可以基于 Node.js fs 模块实现
 */
actual class DefaultFileSystem actual constructor(private val projectPath: String) : ProjectFileSystem {
    
    actual override fun getProjectPath(): String? = projectPath
    
    actual override fun readFile(path: String): String? {
        // TODO: 使用 Node.js fs.readFileSync 实现
        console.warn("File system not implemented for JS platform")
        return null
    }
    
    actual override fun exists(path: String): Boolean {
        // TODO: 使用 Node.js fs.existsSync 实现
        return false
    }

    actual override fun isDirectory(path: String): Boolean {
        // TODO: 使用 Node.js fs.statSync 实现
        return false
    }
    
    actual override fun listFiles(path: String, pattern: String?): List<String> {
        // TODO: 使用 Node.js fs.readdirSync 实现
        return emptyList()
    }
    
    actual override fun searchFiles(pattern: String, maxDepth: Int, maxResults: Int): List<String> {
        // TODO: 使用 Node.js 递归搜索实现
        return emptyList()
    }
    
    actual override fun resolvePath(relativePath: String): String {
        // TODO: 使用 Node.js path.resolve 实现
        return if (relativePath.startsWith("/")) {
            relativePath
        } else {
            "$projectPath/$relativePath"
        }
    }
}

