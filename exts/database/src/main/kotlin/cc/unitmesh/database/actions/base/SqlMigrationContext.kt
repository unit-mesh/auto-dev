package cc.unitmesh.database.actions.base

import cc.unitmesh.devti.template.context.TemplateContext

data class SqlMigrationContext(
    val lang: String = "",
    var sql: String = "",
) : TemplateContext