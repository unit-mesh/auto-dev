package cc.unitmesh.database.provider

import cc.unitmesh.database.util.DatabaseSchemaAssistant
import cc.unitmesh.devti.mcp.host.AbstractMcpTool
import cc.unitmesh.devti.mcp.host.NoArgs
import cc.unitmesh.devti.mcp.host.Response
import com.intellij.openapi.project.Project

class DatabaseMcpToolProvider : AbstractMcpTool<NoArgs>() {
    override val name: String = "get_current_database_schema"

    override val description: String = """
        Get the current database schema which connect in IntelliJ IDEA.
    """.trimIndent()

    override fun handle(project: Project, args: NoArgs): Response {
        val listSchemas = DatabaseSchemaAssistant.listSchemas(project)
        return Response(listSchemas)
    }
}
