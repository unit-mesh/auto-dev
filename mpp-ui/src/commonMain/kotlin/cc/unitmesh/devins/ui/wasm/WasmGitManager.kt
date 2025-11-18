package cc.unitmesh.devins.ui.wasm

import cc.unitmesh.agent.platform.GitOperations

/**
 * WASM Git 操作单例管理器
 * 
 * 用于管理全局共享的 GitOperations 实例，避免重复创建导致的问题：
 * - 避免重复初始化 wasm-git 模块（耗时且可能出错）
 * - 保持工作目录状态（避免目录丢失）
 * - 复用已克隆的仓库数据
 * 
 * 使用示例：
 * ```kotlin
 * val gitOps = WasmGitManager.getInstance()
 * gitOps.performClone(url, dir)
 * ```
 */
object WasmGitManager {
    private var instance: GitOperations? = null
    private const val DEFAULT_WORKSPACE = "/workspace"
    
    /**
     * 获取或创建全局共享的 GitOperations 实例
     * 
     * @param projectPath 工作目录路径，默认为 /workspace
     * @return GitOperations 实例
     */
    fun getInstance(projectPath: String = DEFAULT_WORKSPACE): GitOperations {
        if (instance == null) {
            instance = GitOperations(projectPath = projectPath)
        }
        return instance!!
    }
    
    /**
     * 重置实例（用于测试或需要完全重新初始化的场景）
     */
    fun reset() {
        instance = null
    }
}

