package cc.unitmesh.agent.platform

/**
 * 跨平台 Git 操作抽象
 * 
 * 提供获取修改文件列表和文件差异的能力
 * 不同平台有不同实现：
 * - JVM: 使用 ProcessBuilder 调用 git 命令
 * - Android: 空实现或抛出异常（Android 上通常没有 git）
 * - JS/Wasm: 空实现或抛出异常
 */
expect class GitOperations(projectPath: String) {
    /**
     * 获取 git 仓库中已修改的文件列表
     * @return 文件路径列表
     */
    suspend fun getModifiedFiles(): List<String>
    
    /**
     * 获取指定文件的 diff
     * @param filePath 文件路径
     * @return diff 内容，如果获取失败返回 null
     */
    suspend fun getFileDiff(filePath: String): String?
    
    /**
     * 检查当前平台是否支持 git 操作
     * @return true 表示支持，false 表示不支持
     */
    fun isSupported(): Boolean
}
