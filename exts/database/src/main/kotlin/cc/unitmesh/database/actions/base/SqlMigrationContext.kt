package cc.unitmesh.database.actions.base

data class SqlMigrationContext(
    val lang: String = "",
    var sql: String = "",
)