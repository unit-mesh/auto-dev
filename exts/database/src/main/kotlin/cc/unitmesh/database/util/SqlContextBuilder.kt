package cc.unitmesh.database.util

import com.intellij.database.console.JdbcConsoleProvider
import com.intellij.database.model.ObjectKind
import com.intellij.database.model.basic.BasicModel
import com.intellij.database.model.basic.BasicSchema
import com.intellij.database.model.basic.BasicTable
import com.intellij.database.psi.DbDataSource
import com.intellij.database.util.ObjectPath
import com.intellij.database.util.QNameUtil
import com.intellij.sql.psi.SqlFile

object SqlContextBuilder {
    fun getCurrentNamespace(sqlFile: SqlFile): ObjectPath? {
        val console = JdbcConsoleProvider.getValidConsole(sqlFile.project, sqlFile.virtualFile)
        return console?.currentNamespace
    }

    fun getSchema(ds: DbDataSource?, currentNamespace: ObjectPath?): BasicSchema? {
        val basicModel = ds?.model as? BasicModel ?: return null
        val dasObject = QNameUtil.findByPath(basicModel, currentNamespace).firstOrNull() ?: return null
        return dasObject as? BasicSchema
    }

    fun formatSchema(schema: BasicSchema): String? {
        return schema.familyOf(ObjectKind.TABLE)?.jbi()
            ?.mapNotNull { it as? BasicTable }
            ?.joinToString("\n\n") { describeTable(it) }
    }

    private fun describeTable(table: BasicTable): String =
        """
        |create table ${table.name} {
        |  ${table.columns.joinToString(",\n  ") { "${it.name} ${columnType(it)}" }}
        |}
        """.trimMargin()
}

