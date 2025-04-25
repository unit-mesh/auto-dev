package cc.unitmesh.devti.custom.schema

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import com.jetbrains.jsonSchema.extension.SchemaType

const val MCP_SERVERS_FILE_NAME = "AutoDevMCPServersFile.mcp.json"

class CustomMCPServersSchemaFileProvider(project: Project) : JsonSchemaFileProvider {
    override fun isAvailable(file: VirtualFile): Boolean {
        return runReadAction { isAutoDevCustomAgentFile(file) }
    }

    private fun isAutoDevCustomAgentFile(file: VirtualFile): Boolean {
        if (!file.isValid) {
            return false
        }

        return file.name == MCP_SERVERS_FILE_NAME || file.name.endsWith(".mcp.json")
    }

    override fun getName(): String = "AutoDevMCPServersFile"

    override fun getSchemaFile(): VirtualFile? {
        return JsonSchemaProviderFactory.getResourceFile(this::class.java, customMcpServerSchemaFile)
    }

    override fun getSchemaType(): SchemaType {
        return SchemaType.schema
    }

    companion object {
        private const val customMcpServerSchemaFile = "autodev-mcp-server-schema.json"
    }
}
