package cc.unitmesh.devti.language.ast

import cc.unitmesh.devti.language.ast.action.PatternActionFunc

class ShirePsiQueryStatement(
    val from: List<VariableElement>,
    val where: Statement,
    val select: List<Statement>,
) {
    override fun toString(): String {
        return """
            from {
                ${from.joinToString(", ")} 
            }
            where {
                $where
            } 
            select ${select.joinToString(", ")}"""
            .trimIndent()
    }

    fun toPatternActionFunc(): List<PatternActionFunc> {
        val selectFunc = PatternActionFunc.From(from)
        val whereFunc = PatternActionFunc.Where(where)
        val selectFuncs = PatternActionFunc.Select(select)
        return listOf(selectFunc, whereFunc, selectFuncs)
    }
}

class VariableElement(
    val variableType: String,
    val value: String,
) {
    override fun toString(): String {
        return "$variableType $value"
    }
}
