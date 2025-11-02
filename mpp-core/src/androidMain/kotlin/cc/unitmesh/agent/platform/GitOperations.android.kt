package cc.unitmesh.agent.platform

/**
 * Android 平台的 Git 操作实现
 * 
 * Android 上通常没有 git 命令行工具，因此返回空结果
 * ErrorRecoveryAgent 在 Android 上会自动跳过 git 相关分析
 */
actual class GitOperations actual constructor(private val projectPath: String) {
    
    actual suspend fun getModifiedFiles(): List<String> {
        println("   ⚠️  Git operations not supported on Android")
        return emptyList()
    }
    
    actual suspend fun getFileDiff(filePath: String): String? {
        return null
    }
    
    actual fun isSupported(): Boolean = false
}
