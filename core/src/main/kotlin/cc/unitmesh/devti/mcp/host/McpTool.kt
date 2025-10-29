// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cc.unitmesh.devti.mcp.host

import cc.unitmesh.devti.agent.tool.Tool
import kotlinx.serialization.Serializable
import com.intellij.openapi.project.Project
import kotlin.reflect.KClass

interface McpTool<Args : Any>: Tool {
    fun handle(project: Project, args: Args): Response
}

abstract class AbstractMcpTool<Args : Any> : McpTool<Args> {
    val argKlass: KClass<Args> by lazy {
        val supertype = this::class.supertypes.find {
            it.classifier == AbstractMcpTool::class
        } ?: error("Cannot find McpTool supertype")

        val typeArgument = supertype.arguments.first().type
            ?: error("Cannot find type argument for McpTool")

        @Suppress("UNCHECKED_CAST")
        typeArgument.classifier as KClass<Args>
    }
}

@Serializable
object NoArgs

@Serializable
data class ToolInfo(
    val name: String,
    val description: String,
    val inputSchema: JsonSchemaObject
)

@Serializable
data class Response(
    val status: String? = null,
    val error: String? = null
)

@Serializable
data class JsonSchemaObject(
    val type: String,
    val properties: Map<String, PropertySchema> = emptyMap(),
    val required: List<String> = emptyList(),
    val items: PropertySchema? = null
)

@Serializable
data class PropertySchema(
    val type: String,
    val description: String? = null
)
