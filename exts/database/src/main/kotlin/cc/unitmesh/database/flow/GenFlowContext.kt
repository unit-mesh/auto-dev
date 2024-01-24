package cc.unitmesh.database.flow

data class GenFlowContext(
    val requirement: String,
    val databaseVersion: String,
    val schemaName: String,
    val tableNames: List<String>,
    // for step 2
    var tableInfos: List<String> = emptyList(),
)