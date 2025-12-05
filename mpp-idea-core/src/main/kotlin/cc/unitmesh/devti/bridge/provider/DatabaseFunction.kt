package cc.unitmesh.devti.bridge.provider

enum class DatabaseFunction(val funName: String) {
    Schema("schema"),
    Table("table"),
    Column("column"),
    Query("query")
    ;

    companion object {
        fun fromString(value: String): DatabaseFunction? {
            return entries.firstOrNull { it.funName == value }
        }

        fun allFuncNames(): List<String> = entries.map(DatabaseFunction::funName)
    }
}