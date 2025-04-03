package cc.unitmesh.database.provider

import cc.unitmesh.database.util.DatabaseSchemaAssistant
import cc.unitmesh.database.util.DatabaseSchemaAssistant.getTableColumn
import cc.unitmesh.devti.agent.tool.AgentTool
import cc.unitmesh.devti.bridge.provider.DatabaseFunction
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import com.intellij.database.model.DasTable
import com.intellij.database.model.RawDataSource
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe

class DatabaseFunctionProvider : ToolchainFunctionProvider {
    override suspend fun toolInfos(project: Project): List<AgentTool> {
        val example = BuiltinCommand.example("database")
        return listOf(AgentTool("database", "Database schema and query tool", example))
    }

    override suspend fun isApplicable(project: Project, funcName: String): Boolean =
        DatabaseFunction.entries.any { it.funName == funcName }

    override suspend fun funcNames(): List<String> = DatabaseFunction.allFuncNames()

    override suspend fun execute(
        project: Project,
        prop: String,
        args: List<Any>,
        allVariables: Map<String, Any?>,
        commandName: @NlsSafe String,
    ): Any {
        val databaseFunction = DatabaseFunction.fromString(prop)
            ?: throw IllegalArgumentException("[Database]: Invalid Database function name")

        return when (databaseFunction) {
            DatabaseFunction.Schema -> DatabaseSchemaAssistant.listSchemas(project)
            DatabaseFunction.Table -> executeTableFunction(args, project)
            DatabaseFunction.Column -> executeColumnFunction(args, project)
            DatabaseFunction.Query -> executeSqlFunction(args, project)
        }
    }

    private fun executeTableFunction(args: List<Any>, project: Project): String {
        if (args.isEmpty()) {
            val dataSource = DatabaseSchemaAssistant.allRawDatasource(project).firstOrNull()
                ?: return "[Database]: No database found"
            return DatabaseSchemaAssistant.getTableByDataSource(dataSource).joinToString("\n") {
                it.toString()
            }
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

        if (result.isEmpty()) {
            return "[Database]: Table not found"
        }

        return result.joinToString("\n") {
            it.toString()
        }
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
                return getTableColumn(first)
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
                            getTableColumn(it as DasTable)
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
                            getTableColumn(it)
                        }
                    }
                } else {
                    val dasTable = allTables.firstOrNull { table ->
                        table.name == first
                    }

                    return dasTable?.let {
                        getTableColumn(it)
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
