package cc.unitmesh.database.flow

data class GenSqlContext(
    val requirement: String,
    val databaseVersion: String,
    val schemaName: String,
    val tableNames: List<String>,
    /**
     * Step 2.
     * A list of table names to retrieve the columns from.
     */
    var tableInfos: List<String> = emptyList(),
)