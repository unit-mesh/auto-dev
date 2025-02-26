package cc.unitmesh.devti.bridge

enum class DatabaseFunction(val funName: String) {
    Schema("schema"),
    Table("table"),
    Column("column"),
    Query("query")
    ;

    companion object {
        fun fromString(value: String): DatabaseFunction? {
            return values().firstOrNull { it.funName == value }
        }

        fun allFuncNames(): List<String> = values().map { it.funName }
    }
}