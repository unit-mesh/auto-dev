package cc.unitmesh.database.util

import com.intellij.database.model.DasTable
import com.intellij.database.model.ObjectKind
import com.intellij.database.model.RawDataSource
import com.intellij.database.psi.DbDataSource
import com.intellij.database.psi.DbPsiFacade
import com.intellij.database.util.DasUtil
import com.intellij.openapi.project.Project
import com.intellij.sql.isNullOr

object DatabaseSchemaAssistant {
    fun getDataSources(project: Project): List<DbDataSource> = DbPsiFacade.getInstance(project).dataSources.toList()

    fun allRawDatasource(project: Project): List<RawDataSource> {
        val dbPsiFacade = DbPsiFacade.getInstance(project)
        return dbPsiFacade.dataSources.map { dataSource ->
            dbPsiFacade.getDataSourceManager(dataSource).dataSources
        }.flatten()
    }

    fun getDatabase(project: Project, dbName: String): RawDataSource? {
        return allRawDatasource(project).firstOrNull { it.name == dbName }
    }

    fun getAllTables(project: Project): List<DasTable> {
        return allRawDatasource(project).map {
            val schemaName = it.name.substringBeforeLast('@')
            DasUtil.getTables(it).filter { table ->
                table.kind == ObjectKind.TABLE && (table.dasParent?.name == schemaName || isSQLiteTable(it, table))
            }
        }.flatten()
    }

    fun getTableByDataSource(dataSource: RawDataSource): List<DasTable> {
        return DasUtil.getTables(dataSource).toList()
    }

    fun getTable(dataSource: RawDataSource, tableName: String): List<DasTable> {
        val dasTables = DasUtil.getTables(dataSource)
        return dasTables.filter { it.name == tableName }.toList()
    }

    fun executeSqlQuery(project: Project, sql: String): String {
        return SQLExecutor.executeSqlQuery(project, sql)
    }

    private fun isSQLiteTable(
        rawDataSource: RawDataSource,
        table: DasTable,
    ) = (rawDataSource.databaseVersion.name == "SQLite" && table.dasParent?.name == "main")

    fun getTableColumns(project: Project, tables: List<String> = emptyList()): List<String> {
        val dasTables = getAllTables(project)

        if (tables.isEmpty()) {
            return dasTables.map(::displayTable)
        }

        return dasTables.mapNotNull { table ->
            if (tables.contains(table.name)) {
                displayTable(table)
            } else {
                null
            }
        }
    }

    fun getTableColumn(table: DasTable): String = displayTable(table)

    private fun displayTable(table: DasTable): String {
        val dasColumns = DasUtil.getColumns(table)
        val columns = dasColumns.joinToString(",") { column ->
            "${column.name} ${column.dasType.toDataType()}${if (column.isNullOr("")) "" else " NOT NULL"}"
        }

        return """
    CREATE TABLE ${table.name} (
            $columns
        );
    """.trimIndent()
    }
}