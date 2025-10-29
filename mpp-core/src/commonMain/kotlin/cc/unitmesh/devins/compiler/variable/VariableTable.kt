package cc.unitmesh.devins.compiler.variable

/**
 * 变量表
 * 参考 @exts/devins-lang/src/main/kotlin/cc/unitmesh/devti/language/ast/variable/VariableTable.kt
 */
class VariableTable {
    private val table: MutableMap<String, VariableInfo> = mutableMapOf()

    /**
     * 添加变量
     */
    fun addVariable(
        name: String, 
        varType: VariableType, 
        value: Any? = null,
        scope: VariableScope = VariableScope.BUILTIN
    ) {
        var varName = name
        
        // 处理 {context.frameworkContext} 格式
        if (varName.startsWith("{") && varName.endsWith("}")) {
            varName = varName.substring(1, varName.length - 1)
        }

        // 移除 context 前缀
        if (varName.startsWith("context.")) {
            varName = varName.substring(8)
        }

        if (!table.containsKey(varName)) {
            table[varName] = VariableInfo(varType, scope, value)
        } else {
            // 忽略重复的键以避免启动失败
            // 影响：lineDeclared 值仅对第一次有效，但目前还没有在任何地方使用
        }
    }

    /**
     * 添加另一个变量表的所有变量
     */
    fun addVariable(variableTable: VariableTable) {
        variableTable.getAllVariables().forEach {
            table[it.key] = it.value
        }
    }

    /**
     * 获取变量
     */
    fun getVariable(name: String): VariableInfo? {
        return table[name]
    }

    /**
     * 更新变量
     */
    fun updateVariable(
        name: String, 
        newType: VariableType? = null, 
        newScope: VariableScope? = null, 
        newValue: Any? = null
    ) {
        val variable = table[name] ?: throw Exception("Variable $name not found.")
        val updatedVariable = variable.copy(
            type = newType ?: variable.type,
            scope = newScope ?: variable.scope,
            value = newValue ?: variable.value
        )
        table[name] = updatedVariable
    }

    /**
     * 移除变量
     */
    fun removeVariable(name: String) {
        if (table.containsKey(name)) {
            table.remove(name)
        } else {
            throw Exception("Variable $name not found.")
        }
    }

    /**
     * 获取所有变量
     */
    fun getAllVariables(): Map<String, VariableInfo> = table.toMap()

    /**
     * 检查变量是否存在
     */
    fun hasVariable(name: String): Boolean = table.containsKey(name)

    /**
     * 获取变量数量
     */
    fun size(): Int = table.size

    /**
     * 清空变量表
     */
    fun clear() {
        table.clear()
    }

    /**
     * 获取变量名列表
     */
    fun getVariableNames(): Set<String> = table.keys

    /**
     * 获取指定作用域的变量
     */
    fun getVariablesByScope(scope: VariableScope): Map<String, VariableInfo> {
        return table.filter { it.value.scope == scope }
    }

    /**
     * 获取指定类型的变量
     */
    fun getVariablesByType(type: VariableType): Map<String, VariableInfo> {
        return table.filter { it.value.type == type }
    }
}

/**
 * 变量信息
 */
data class VariableInfo(
    val type: VariableType,
    val scope: VariableScope,
    val value: Any? = null,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * 变量类型
 */
enum class VariableType {
    STRING,
    BOOLEAN,
    NUMBER,
    OBJECT,
    ARRAY,
    FUNCTION,
    UNKNOWN
}

/**
 * 变量作用域
 */
enum class VariableScope {
    BUILTIN,        // 内置变量
    USER_DEFINED,   // 用户定义变量
    GLOBAL,         // 全局变量
    LOCAL           // 局部变量
}
