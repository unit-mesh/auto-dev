package cc.unitmesh.devins.compiler.service

import cc.unitmesh.devins.compiler.result.DevInsCompiledResult
import cc.unitmesh.devins.filesystem.ProjectFileSystem
import kotlin.concurrent.Volatile

/**
 * DevIns 编译器服务接口
 *
 * 提供可切换的编译器核心，支持：
 * - mpp-core 默认实现（跨平台，基于自定义 AST）
 * - IDEA 专用实现（基于 PSI，支持 IDE 功能如 Symbol 解析、重构等）
 *
 * 使用方式：
 * ```kotlin
 * // 在 mpp-idea 中使用 IDEA 编译器
 * val compilerService = IdeaDevInsCompilerService(project)
 * val llmService = KoogLLMService(config, compilerService = compilerService)
 *
 * // 在 CLI/Desktop 中使用默认编译器
 * val llmService = KoogLLMService(config)
 * // compilerService 默认为 DefaultDevInsCompilerService
 * ```
 */
interface DevInsCompilerService {

    /**
     * 编译 DevIns 源代码
     *
     * @param source DevIns 源代码字符串
     * @param fileSystem 项目文件系统，用于解析文件路径
     * @return 编译结果
     */
    suspend fun compile(source: String, fileSystem: ProjectFileSystem): DevInsCompiledResult

    /**
     * 编译 DevIns 源代码，带有自定义变量
     *
     * @param source DevIns 源代码字符串
     * @param fileSystem 项目文件系统
     * @param variables 自定义变量映射
     * @return 编译结果
     */
    suspend fun compile(
        source: String,
        fileSystem: ProjectFileSystem,
        variables: Map<String, Any>
    ): DevInsCompiledResult

    /**
     * 检查编译器是否支持 IDE 功能
     *
     * IDE 功能包括：
     * - Symbol 解析 (/symbol 命令)
     * - 代码重构 (/refactor 命令)
     * - 数据库操作 (/database 命令)
     * - 代码结构分析 (/structure 命令)
     * - 符号使用查找 (/usage 命令)
     *
     * @return true 如果支持 IDE 功能
     */
    fun supportsIdeFeatures(): Boolean = false

    /**
     * 获取编译器名称，用于日志和调试
     */
    fun getName(): String

    companion object {
        /**
         * 全局编译器服务实例
         * 可以在应用启动时设置为 IDEA 专用实现
         */
        @Volatile
        private var instance: DevInsCompilerService? = null

        /**
         * 获取当前编译器服务实例
         * 如果未设置，返回默认实现
         */
        fun getInstance(): DevInsCompilerService {
            return instance ?: DefaultDevInsCompilerService()
        }

        /**
         * 设置全局编译器服务实例
         * 应在应用启动时调用
         */
        fun setInstance(service: DevInsCompilerService) {
            instance = service
        }

        /**
         * 重置为默认实现
         */
        fun reset() {
            instance = null
        }
    }
}

