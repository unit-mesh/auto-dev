package cc.unitmesh.devti.custom.schema

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import com.jetbrains.jsonSchema.extension.SchemaType

const val INLAY_PROMPTS_FILE_NAME = "AutoDevInlayCodeCompletePromptFile.json"

//todo: refactor provider to reduce duplicate code
class InlayCodeCompletePromptsSchemaFileProvider(project: Project) : JsonSchemaFileProvider {
    override fun isAvailable(file: VirtualFile): Boolean {
        return runReadAction { isMyFile(file) }
    }

    private fun isMyFile(file: VirtualFile): Boolean {
        if (!file.isValid) {
            return false
        }

        return file.name == INLAY_PROMPTS_FILE_NAME
    }

    override fun getName(): String = "AutoDevInlayCodeCompletePromptFile"

    override fun getSchemaFile(): VirtualFile? {
        return JsonSchemaProviderFactory.getResourceFile(this::class.java, inlayPromptsSchemaFile)
    }

    override fun getSchemaType(): SchemaType {
        return SchemaType.schema
    }

    companion object {
        private const val inlayPromptsSchemaFile = "autodev-inlay-code-complete-prompt-file.json"
    }
}
