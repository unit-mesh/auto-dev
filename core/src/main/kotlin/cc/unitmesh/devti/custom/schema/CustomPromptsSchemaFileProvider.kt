package cc.unitmesh.devti.custom.schema

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import com.jetbrains.jsonSchema.extension.SchemaType

const val CUSTOM_PROMPTS_FILE_NAME = "AutoDevCustomsPromptFile.json"

class CustomPromptsSchemaFileProvider(project: Project) : JsonSchemaFileProvider {
    override fun isAvailable(file: VirtualFile): Boolean {
        return runReadAction { isAutoDevCustomAgentFile(file) }
    }

    private fun isAutoDevCustomAgentFile(file: VirtualFile): Boolean {
        if (!file.isValid) {
            return false
        }

        return file.name == CUSTOM_PROMPTS_FILE_NAME
    }

    override fun getName(): String = "AutoDevCustomsPromptFile"

    override fun getSchemaFile(): VirtualFile? {
        return JsonSchemaProviderFactory.getResourceFile(this::class.java, customPromptsSchemaFile)
    }

    override fun getSchemaType(): SchemaType {
        return SchemaType.schema
    }

    companion object {
        private const val customPromptsSchemaFile = "autodev-custom-prompts.json"
    }
}
