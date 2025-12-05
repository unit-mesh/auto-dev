package cc.unitmesh.devti.language.ast

data class ForeignFunction(
    val funcName: String,
    val funcPath: String,
    val accessFuncName: String,
    val inputTypes: List<String>,
    val returnVars: Map<String, Any>,
) {
    companion object {
        fun from(map: Map<String, FrontMatterType>): List<ForeignFunction> {
            return map
                .filter { (_, value) ->
                    value is FrontMatterType.EXPRESSION && value.value is ForeignFunctionStmt
                }
                .map { (key, value) ->
                    val stmt = value.value as ForeignFunctionStmt
                    ForeignFunction(
                        key,
                        stmt.funcPath,
                        stmt.accessFuncName,
                        stmt.inputTypes,
                        stmt.returnVars
                    )
                }
        }
    }
}
