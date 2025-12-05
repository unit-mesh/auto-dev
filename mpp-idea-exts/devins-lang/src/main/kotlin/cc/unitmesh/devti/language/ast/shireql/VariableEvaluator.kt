package cc.unitmesh.devti.language.ast.shireql

import cc.unitmesh.devti.language.ast.FrontMatterType


class VariableEvaluator(val element: Any) {
    val valued: MutableMap<FrontMatterType, Any?> = mutableMapOf()

    fun putValue(key: FrontMatterType, value: Any?) {
        valued[key] = value
    }

    fun getValue(key: FrontMatterType): Any? {
        return valued[key]
    }
}

class VariableContainerManager {
    val variables: MutableMap<Any, VariableEvaluator> = mutableMapOf()

    fun putValue(key: Any, prop: FrontMatterType, value: Any) {
        if (!variables.containsKey(key)) {
            variables[key] = VariableEvaluator(value)
        }

        variables[key]?.putValue(prop, value)
    }

    fun getValue(key: Any, prop: FrontMatterType): Any? {
        return variables[key].let {
            it?.getValue(prop)
        }
    }

    fun isNotEmpty(): Boolean {
        return variables.isNotEmpty()
    }
}