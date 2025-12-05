package cc.unitmesh.devti.language.ast.variable

class VariableTable {
    private val table: MutableMap<String, VariableInfo> = mutableMapOf()

    fun addVariable(name: String, varType: VariableType, lineDeclared: Int, scope: VariableScope = VariableScope.BuiltIn) {
        var varName = name;
        // {context.frameworkContext}
        if (varName.startsWith("{") && varName.endsWith("}")) {
            varName = varName.substring(1, varName.length - 1)
        }

        // remove the context prefix
        if (varName.startsWith("context.")) {
            varName = varName.substring(8)
        }

        if (!table.containsKey(varName)) {
            table[varName] = VariableInfo(varType, scope, lineDeclared)
        } else {
            // Ignore duplicate keys to avoid startup failures.
            // Affected: the lineDeclared value is only valid for the first time, but it is not used anywhere yet.
//            throw Exception("Variable $varName already declared.")
        }
    }

    fun addVariable(variableTable: VariableTable) {
        variableTable.getAllVariables().forEach {
            table[it.key] = it.value
        }
    }

    fun getVariable(name: String): VariableInfo {
        return table[name] ?: throw Exception("Variable $name not found.")
    }

    fun updateVariable(name: String, newType: VariableType? = null, newScope: VariableScope? = null, newLineDeclared: Int? = null) {
        val variable = table[name] ?: throw Exception("Variable $name not found.")
        val updatedVariable = variable.copy(
            type = newType ?: variable.type,
            scope = newScope ?: variable.scope,
            lineDeclared = newLineDeclared ?: variable.lineDeclared
        )
        table[name] = updatedVariable
    }

    fun removeVariable(name: String) {
        if (table.containsKey(name)) {
            table.remove(name)
        } else {
            throw Exception("Variable $name not found.")
        }
    }

    fun getAllVariables(): Map<String, VariableInfo> = table.toMap()

    data class VariableInfo(
        val type: VariableType,
        val scope: VariableScope,
        val lineDeclared: Int
    )

    enum class VariableType {
        String,
        Boolean,
        Number,
    }

    enum class VariableScope {
        BuiltIn,
        UserDefined
    }
}