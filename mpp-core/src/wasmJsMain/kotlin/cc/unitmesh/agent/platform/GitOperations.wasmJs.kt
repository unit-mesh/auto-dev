package cc.unitmesh.agent.platform

/**
 * WebAssembly 平台的 Git 操作实现 (Stub)
 * 
 * WASM 环境中无法直接调用 git 命令，提供空实现
 */
actual class GitOperations actual constructor(private val projectPath: String) {
    
    actual fun isSupported(): Boolean {
        // WASM environment doesn't have access to git
        return false
    }
    
    actual suspend fun getModifiedFiles(): List<String> {
        // WASM environment doesn't have access to git
        return emptyList()
    }
    
    actual suspend fun getFileDiff(filePath: String): String? {
        // WASM environment doesn't have access to git
        return null
    }
}
