package cc.unitmesh.devins.compiler

import cc.unitmesh.devins.compiler.context.CompilerContext
import cc.unitmesh.devins.compiler.context.CompilerOptions
import cc.unitmesh.devins.compiler.result.DevInsCompiledResult
import cc.unitmesh.devins.compiler.variable.VariableTable
import cc.unitmesh.devins.compiler.variable.VariableType
import cc.unitmesh.devins.compiler.variable.VariableScope
import cc.unitmesh.devins.workspace.WorkspaceManager

/**
 * DevIns 编译器门面
 * 提供简化的 API 来使用 DevIns 编译器
 */
object DevInsCompilerFacade {
    
    /**
     * 编译 DevIns 源代码为模板
     */
    suspend fun compile(source: String): DevInsCompiledResult {
        val compiler = DevInsCompiler.create()
        return compiler.compileFromSource(source)
    }
    
    /**
     * 编译 DevIns 源代码为模板，带有自定义变量
     */
    suspend fun compile(source: String, variables: Map<String, Any>): DevInsCompiledResult {
        val context = CompilerContext()
        
        // 从 WorkspaceManager 获取文件系统
        val workspace = WorkspaceManager.getCurrentOrEmpty()
        context.fileSystem = workspace.fileSystem
        
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
    
    /**
     * 编译 DevIns 源代码为模板，带有自定义选项
     */
    suspend fun compile(
        source: String, 
        options: CompilerOptions = CompilerOptions(),
        variables: Map<String, Any> = emptyMap()
    ): DevInsCompiledResult {
        val context = CompilerContext()
        
        // 从 WorkspaceManager 获取文件系统
        val workspace = WorkspaceManager.getCurrentOrEmpty()
        context.fileSystem = workspace.fileSystem
        
        // 更新编译器选项
        context.options = options
        
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
    
    /**
     * 编译 DevIns 源代码为模板，使用提供的上下文
     * 这对于需要自定义文件系统或其他上下文配置的场景非常有用（如 SpecKit 支持）
     */
    suspend fun compile(
        source: String,
        context: CompilerContext
    ): DevInsCompiledResult {
        val compiler = DevInsCompiler(context)
        return compiler.compileFromSource(source)
    }
    
    /**
     * 编译 DevIns 源代码为原始输出（不进行模板处理）
     */
    suspend fun compileRaw(source: String): DevInsCompiledResult {
        val options = CompilerOptions(enableTemplateCompilation = false)
        return compile(source, options)
    }
    
    /**
     * 编译 DevIns 源代码并返回最终的字符串结果
     */
    suspend fun compileToString(source: String): String {
        val result = compile(source)
        return if (result.isSuccess()) {
            result.output
        } else {
            throw CompilationException("Compilation failed: ${result.getError()}")
        }
    }
    
    /**
     * 编译 DevIns 源代码并返回最终的字符串结果，带有变量
     */
    suspend fun compileToString(source: String, variables: Map<String, Any>): String {
        val result = compile(source, variables)
        return if (result.isSuccess()) {
            result.output
        } else {
            throw CompilationException("Compilation failed: ${result.getError()}")
        }
    }
    
    /**
     * 验证 DevIns 源代码语法
     */
    suspend fun validate(source: String): ValidationResult {
        val result = compileRaw(source)
        return ValidationResult(
            isValid = result.isSuccess(),
            errorMessage = result.errorMessage,
            warnings = emptyList() // TODO: 实现警告收集
        )
    }
    
    /**
     * 创建编译器构建器
     */
    fun builder(): DevInsCompilerBuilder {
        return DevInsCompilerBuilder()
    }
    
    /**
     * 推断变量类型
     */
    private fun inferVariableType(value: Any?): VariableType {
        return when (value) {
            is String -> VariableType.STRING
            is Boolean -> VariableType.BOOLEAN
            is Number -> VariableType.NUMBER
            is List<*> -> VariableType.ARRAY
            is Map<*, *> -> VariableType.OBJECT
            else -> VariableType.UNKNOWN
        }
    }
}

/**
 * DevIns 编译器构建器
 */
class DevInsCompilerBuilder {
    private var currentOptions = CompilerOptions()
    private val variables = mutableMapOf<String, Any>()

    fun debug(enable: Boolean = true): DevInsCompilerBuilder {
        currentOptions = currentOptions.copy(debug = enable)
        return this
    }

    fun strict(enable: Boolean = true): DevInsCompilerBuilder {
        currentOptions = currentOptions.copy(strict = enable)
        return this
    }

    fun maxRecursionDepth(depth: Int): DevInsCompilerBuilder {
        currentOptions = currentOptions.copy(maxRecursionDepth = depth)
        return this
    }

    fun enableTemplateCompilation(enable: Boolean = true): DevInsCompilerBuilder {
        currentOptions = currentOptions.copy(enableTemplateCompilation = enable)
        return this
    }

    fun keepRawOutput(keep: Boolean = true): DevInsCompilerBuilder {
        currentOptions = currentOptions.copy(keepRawOutput = keep)
        return this
    }
    
    fun variable(name: String, value: Any): DevInsCompilerBuilder {
        variables[name] = value
        return this
    }
    
    fun variables(vars: Map<String, Any>): DevInsCompilerBuilder {
        variables.putAll(vars)
        return this
    }
    
    suspend fun compile(source: String): DevInsCompiledResult {
        return DevInsCompilerFacade.compile(source, currentOptions, variables)
    }
    
    suspend fun compileToString(source: String): String {
        val result = compile(source)
        return if (result.isSuccess()) {
            result.output
        } else {
            throw CompilationException("Compilation failed: ${result.getError()}")
        }
    }
}

/**
 * 验证结果
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val warnings: List<String> = emptyList()
)

/**
 * 编译异常
 */
class CompilationException(message: String, cause: Throwable? = null) : Exception(message, cause)
