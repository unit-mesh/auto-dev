package cc.unitmesh.devins.compiler.service

import cc.unitmesh.devins.compiler.DevInsCompiler
import cc.unitmesh.devins.compiler.context.CompilerContext
import cc.unitmesh.devins.compiler.result.DevInsCompiledResult
import cc.unitmesh.devins.compiler.variable.VariableScope
import cc.unitmesh.devins.compiler.variable.VariableType
import cc.unitmesh.devins.filesystem.ProjectFileSystem

/**
 * 默认的 DevIns 编译器服务实现
 *
 * 使用 mpp-core 的 DevInsCompiler，基于自定义 AST 解析器。
 * 适用于 CLI、Desktop、WASM 等跨平台环境。
 *
 * 特点：
 * - 跨平台支持（JS, WASM, Desktop JVM, Android, iOS）
 * - 基于自定义 DevInsParser 解析
 * - 命令输出为占位符格式（如 {{FILE_CONTENT:path}}）
 * - 不支持 IDE 特定功能（Symbol 解析、重构等）
 */
class DefaultDevInsCompilerService : DevInsCompilerService {

    override suspend fun compile(source: String, fileSystem: ProjectFileSystem): DevInsCompiledResult {
        val context = CompilerContext().apply {
            this.fileSystem = fileSystem
        }
        val compiler = DevInsCompiler(context)
        return compiler.compileFromSource(source)
    }

    override suspend fun compile(
        source: String,
        fileSystem: ProjectFileSystem,
        variables: Map<String, Any>
    ): DevInsCompiledResult {
        val context = CompilerContext().apply {
            this.fileSystem = fileSystem
        }

        // 添加自定义变量
        variables.forEach { (name, value) ->
            context.variableTable.addVariable(
                name = name,
                varType = inferVariableType(value),
                value = value,
                scope = VariableScope.USER_DEFINED
            )
        }

        val compiler = DevInsCompiler(context)
        return compiler.compileFromSource(source)
    }

    override fun supportsIdeFeatures(): Boolean = false

    override fun getName(): String = "DefaultDevInsCompilerService (mpp-core)"

    private fun inferVariableType(value: Any): VariableType {
        return when (value) {
            is String -> VariableType.STRING
            is Int, is Long, is Double, is Float -> VariableType.NUMBER
            is Boolean -> VariableType.BOOLEAN
            is List<*> -> VariableType.ARRAY
            is Map<*, *> -> VariableType.OBJECT
            else -> VariableType.UNKNOWN
        }
    }
}

