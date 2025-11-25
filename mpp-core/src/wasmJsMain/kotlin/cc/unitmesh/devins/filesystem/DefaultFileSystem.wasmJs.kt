package cc.unitmesh.devins.filesystem

/**
 * WebAssembly 平台的文件系统实现
 * 目前提供空实现，未来可以基于虚拟文件系统实现
 */
actual class DefaultFileSystem actual constructor(private val projectPath: String) : ProjectFileSystem {
    
    actual override fun getProjectPath(): String? = projectPath
    
    actual override fun readFile(path: String): String? {
        // TODO: 实现 WASM 文件系统支持
        println("File system not implemented for WASM platform")
        return null
    }

    actual override fun readFileAsBytes(path: String): ByteArray? {
        // TODO: 实现 WASM 文件系统支持
        println("File system not implemented for WASM platform")
        return null
    }

    actual override fun writeFile(path: String, content: String): Boolean {
        // TODO: 实现 WASM 文件系统支持
        println("File system not implemented for WASM platform")
        return false
    }

    actual override fun exists(path: String): Boolean {
        return false
    }

    actual override fun isDirectory(path: String): Boolean {
        return false
    }
    
    actual override fun listFiles(path: String, pattern: String?): List<String> {
        return emptyList()
    }
    
    actual override fun searchFiles(pattern: String, maxDepth: Int, maxResults: Int): List<String> {
        return emptyList()
    }
    
    actual override fun resolvePath(relativePath: String): String {
        return if (relativePath.startsWith("/")) {
            relativePath
        } else {
            "$projectPath/$relativePath"
        }
    }

    actual override fun createDirectory(path: String): Boolean {
        // TODO: 实现 WASM 文件系统支持
        println("File system not implemented for WASM platform")
        return false
    }
}

