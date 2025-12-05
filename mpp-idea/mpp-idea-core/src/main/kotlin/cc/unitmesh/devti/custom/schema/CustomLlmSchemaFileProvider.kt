package cc.unitmesh.devti.custom.schema

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.SchemaType

const val AUTODEV_CUSTOM_LLM_FILE = "AutoDevCustomLlmFile.json"

class CustomLlmSchemaFileProvider(val project: Project) : JsonSchemaFileProvider {
    override fun isAvailable(file: VirtualFile): Boolean {
        return runReadAction { isAutoDevCustomAgentFile(file) }
    }

    private fun isAutoDevCustomAgentFile(file: VirtualFile): Boolean {
        if (!file.isValid) return false
        return file.name == AUTODEV_CUSTOM_LLM_FILE
    }

    override fun getName(): String = "AutoDevCustomLlmFile"
    override fun getSchemaFile() = VfsUtil.findFileByURL(javaClass.getResource(CUSTOM_LLM_SCHEMA_FILE)!!)
    override fun getSchemaType(): SchemaType = SchemaType.embeddedSchema

    companion object {
        private const val CUSTOM_LLM_SCHEMA_FILE = "autodev-custom-llms.json"
    }
}