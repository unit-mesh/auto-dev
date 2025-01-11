package cc.unitmesh.devti.custom.schema

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.SchemaType

const val CUSTOM_AGENT_FILE_NAME = "AutoDevCustomAgentFile.json"

class CustomAgentSchemaFileProvider(val project: Project): JsonSchemaFileProvider {
    override fun isAvailable(file: VirtualFile): Boolean {
        return runReadAction { isAutoDevCustomAgentFile(file) }
    }

    private fun isAutoDevCustomAgentFile(file: VirtualFile): Boolean {
        if (!file.isValid) {
            return false
        }

        return file.name == CUSTOM_AGENT_FILE_NAME
    }

    override fun getName(): String = "AutoDevCustomAgentFile"
    override fun getSchemaFile() = VfsUtil.findFileByURL(javaClass.getResource(CUSTOM_AGENT_SCHEMA_FILE)!!)
    override fun getSchemaType(): SchemaType = SchemaType.embeddedSchema

    companion object {
        private const val CUSTOM_AGENT_SCHEMA_FILE = "autodev-custom-agent.json"
    }
}