package cc.unitmesh.devti.custom.schema

import com.intellij.openapi.project.Project
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory

class AutoDevPromptJsonSchemaProviderFactory: JsonSchemaProviderFactory {
    override fun getProviders(project: Project): MutableList<JsonSchemaFileProvider> {
        return mutableListOf(CustomPromptsSchemaFileProvider(project))
    }
}