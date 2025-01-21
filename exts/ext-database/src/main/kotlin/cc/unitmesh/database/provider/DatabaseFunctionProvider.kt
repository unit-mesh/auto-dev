package cc.unitmesh.database.provider

import cc.unitmesh.database.util.DatabaseSchemaAssistant
import cc.unitmesh.database.util.DatabaseSchemaAssistant.getTableColumn
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import com.intellij.database.model.DasTable
import com.intellij.database.model.RawDataSource
import com.intellij.database.util.DasUtil
import com.intellij.database.util.DbUtil
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project

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
    }
}

class DatabaseFunctionProvider : ToolchainFunctionProvider {
    override fun isApplicable(project: Project, funcName: String): Boolean {
        return DatabaseFunction.values().any { it.funName == funcName }
    }

    override fun execute(
        project: Project,
        funcName: String,
        args: List<Any>,
        allVariables: Map<String, Any?>,
    ): Any {
        val databaseFunction = DatabaseFunction.fromString(funcName)
            ?: throw IllegalArgumentException("Shire[Database]: Invalid Database function name")

        return when (databaseFunction) {
            DatabaseFunction.Schema -> listSchemas(args, project)
            DatabaseFunction.Table -> executeTableFunction(args, project)
            DatabaseFunction.Column -> executeColumnFunction(args, project)
            DatabaseFunction.Query -> executeSqlFunction(args, project)
        }
    }

    private fun listSchemas(args: List<Any>, project: Project): Any {
        val dataSources = DbUtil.getDataSources(project)
        if (dataSources.isEmpty) return ""

        return dataSources.mapNotNull {
            val tableSchema = DasUtil.getTables(it).toList().mapNotNull<DasTable, String> {
                if (it.dasParent?.name == "information_schema") return@mapNotNull null
                getTableColumn(it)
            }

            if (tableSchema.isEmpty()) return@mapNotNull null
            val name = it.name.substringBeforeLast('@')
            "Database Schema result:\n\n```sql\n# DATABASE NAME: ${name};\n${tableSchema.joinToString("\n")}\n```\n"
        }.joinToString("\n")
    }

    private fun executeTableFunction(args: List<Any>, project: Project): Any {
        if (args.isEmpty()) {
            val dataSource = DatabaseSchemaAssistant.allRawDatasource(project).firstOrNull()
                ?: return "ShireError[Database]: No database found"
            return DatabaseSchemaAssistant.getTableByDataSource(dataSource)
        }

        val dbName = args.first()
        // for example: [accounts, payment_limits, transactions]
        var result = mutableListOf<DasTable>()
        when (dbName) {
            is String -> {
                if (dbName.startsWith("[") && dbName.endsWith("]")) {
                    val tableNames = dbName.substring(1, dbName.length - 1).split(",")
                    result = tableNames.map {
                        getTable(project, it.trim())
                    }.flatten().toMutableList()
                } else {
                    result = getTable(project, dbName).toMutableList()
                }
            }

            is List<*> -> {
                result = dbName.map {
                    getTable(project, it as String)
                }.flatten().toMutableList()
            }

            else -> {

            }
        }

        return result
    }

    private fun executeSqlFunction(args: List<Any>, project: Project): Any {
        if (args.isEmpty()) {
            return "ShireError[DBTool]: SQL function requires a SQL query"
        }

        val sqlQuery = args.first()
        return DatabaseSchemaAssistant.executeSqlQuery(project, sqlQuery as String)
    }

    private fun executeColumnFunction(args: List<Any>, project: Project): Any {
        if (args.isEmpty()) {
            val allTables = DatabaseSchemaAssistant.getAllTables(project)
            val map = allTables.map {
                getTableColumn(it)
            }
            return """
                |```sql
                |${map.joinToString("\n")}
                |```
            """.trimMargin()
        }

        when (val first = args[0]) {
            is RawDataSource -> {
                return if (args.size == 1) {
                    DatabaseSchemaAssistant.getTableByDataSource(first)
                } else {
                    DatabaseSchemaAssistant.getTable(first, args[1] as String)
                }
            }

            is DasTable -> {
                return DatabaseSchemaAssistant.getTableColumn(first)
            }

            is List<*> -> {
                return when (first.first()) {
                    is RawDataSource -> {
                        return first.map {
                            DatabaseSchemaAssistant.getTableByDataSource(it as RawDataSource)
                        }
                    }

                    is DasTable -> {
                        return first.map {
                            DatabaseSchemaAssistant.getTableColumn(it as DasTable)
                        }
                    }

                    else -> {
                        "ShireError[DBTool]: Table function requires a data source or a list of table names"
                    }
                }
            }

            is String -> {
                val allTables = DatabaseSchemaAssistant.getAllTables(project)
                if (first.startsWith("[") && first.endsWith("]")) {
                    val tableNames = first.substring(1, first.length - 1).split(",")
                    return tableNames.mapNotNull {
                        val dasTable = allTables.firstOrNull { table ->
                            table.name == it.trim()
                        }

                        dasTable?.let {
                            DatabaseSchemaAssistant.getTableColumn(it)
                        }
                    }
                } else {
                    val dasTable = allTables.firstOrNull { table ->
                        table.name == first
                    }

                    return dasTable?.let {
                        DatabaseSchemaAssistant.getTableColumn(it)
                    } ?: "ShireError[DBTool]: Table not found"
                }
            }

            else -> {
                logger<DatabaseFunctionProvider>().error("ShireError[DBTool] args types: ${first.javaClass}")
                return "ShireError[DBTool]: Table function requires a data source or a list of table names"
            }
        }
    }

    private fun getTable(project: Project, dbName: String): List<DasTable> {
        val database = DatabaseSchemaAssistant.getDatabase(project, dbName)
            ?: return emptyList()
        return DatabaseSchemaAssistant.getTableByDataSource(database)
    }
}
