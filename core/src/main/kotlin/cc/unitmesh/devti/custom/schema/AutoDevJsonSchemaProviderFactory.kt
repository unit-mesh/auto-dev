package cc.unitmesh.devti.custom.schema

import cc.unitmesh.devti.mcp.schema.CustomMCPServersSchemaFileProvider
import com.intellij.openapi.project.Project
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory

class AutoDevJsonSchemaProviderFactory : JsonSchemaProviderFactory {
    override fun getProviders(project: Project): MutableList<JsonSchemaFileProvider> {
        return mutableListOf(
            CustomAgentSchemaFileProvider(project),
            CustomLlmSchemaFileProvider(project),
            CustomPromptsSchemaFileProvider(project),
            CustomMCPServersSchemaFileProvider(project)
        )
    }
}