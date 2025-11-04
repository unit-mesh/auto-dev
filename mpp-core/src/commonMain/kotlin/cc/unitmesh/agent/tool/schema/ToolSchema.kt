package cc.unitmesh.agent.tool.schema

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Base interface for tool parameter schemas
 * Inspired by Augment's declarative tool approach
 */
interface ToolSchema {
    /**
     * Generate JSON Schema for AI model consumption
     * Compatible with OpenAI function calling and MCP format
     */
    fun toJsonSchema(): JsonElement
    
    /**
     * Get human-readable parameter description for documentation
     */
    fun getParameterDescription(): String
    
    /**
     * Generate example usage for the tool
     */
    fun getExampleUsage(toolName: String): String
}

/**
 * Property definition for schema properties
 */
@Serializable
data class SchemaProperty(
    val type: String,
    val description: String,
    val required: Boolean = false,
    val default: JsonElement? = null,
    val enum: List<String>? = null,
    val minimum: Int? = null,
    val maximum: Int? = null,
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val pattern: String? = null,
    val format: String? = null,
    val items: SchemaProperty? = null, // For array types
    val properties: Map<String, SchemaProperty>? = null, // For object types
    val additionalProperties: Boolean? = null
) {
    fun toJsonElement(): JsonElement {
        return buildJsonObject {
            put("type", type)
            put("description", description)
            
            default?.let { put("default", it) }
            enum?.let { 
                putJsonArray("enum") {
                    it.forEach { value -> add(JsonPrimitive(value)) }
                }
            }
            minimum?.let { put("minimum", it) }
            maximum?.let { put("maximum", it) }
            minLength?.let { put("minLength", it) }
            maxLength?.let { put("maxLength", it) }
            pattern?.let { put("pattern", it) }
            format?.let { put("format", it) }
            items?.let { put("items", it.toJsonElement()) }
            properties?.let { props ->
                putJsonObject("properties") {
                    props.forEach { (name, schema) ->
                        put(name, schema.toJsonElement())
                    }
                }
            }
            additionalProperties?.let { put("additionalProperties", it) }
        }
    }
}

/**
 * Declarative schema builder following Augment's pattern
 */
abstract class DeclarativeToolSchema(
    private val description: String,
    private val properties: Map<String, SchemaProperty>
) : ToolSchema {
    
    private val requiredProperties = properties.filter { it.value.required }.keys.toList()
    
    override fun toJsonSchema(): JsonElement {
        return buildJsonObject {
            put("type", "object")
            put("description", description)
            
            putJsonObject("properties") {
                properties.forEach { (name, property) ->
                    put(name, property.toJsonElement())
                }
            }
            
            if (requiredProperties.isNotEmpty()) {
                putJsonArray("required") {
                    requiredProperties.forEach { add(JsonPrimitive(it)) }
                }
            }
            
            put("additionalProperties", false)
        }
    }
    
    override fun getParameterDescription(): String {
        val params = properties.map { (name, prop) ->
            val requiredText = if (prop.required) " (required)" else " (optional)"
            val defaultText = prop.default?.let { " [default: $it]" } ?: ""
            val enumText = prop.enum?.let { " [${it.joinToString("|")}]" } ?: ""
            "  - $name: ${prop.type}$requiredText$defaultText$enumText - ${prop.description}"
        }.joinToString("\n")
        
        return "$description\n\nParameters:\n$params"
    }
    
    protected fun getRequiredParams(): List<String> = requiredProperties
    protected fun getProperty(name: String): SchemaProperty? = properties[name]
    protected fun getAllProperties(): Map<String, SchemaProperty> = properties
}

/**
 * Helper functions for creating common property types
 */
object SchemaPropertyBuilder {
    
    fun string(
        description: String,
        required: Boolean = false,
        default: String? = null,
        enum: List<String>? = null,
        pattern: String? = null,
        minLength: Int? = null,
        maxLength: Int? = null,
        format: String? = null
    ) = SchemaProperty(
        type = "string",
        description = description,
        required = required,
        default = default?.let { JsonPrimitive(it) },
        enum = enum,
        pattern = pattern,
        minLength = minLength,
        maxLength = maxLength,
        format = format
    )
    
    fun integer(
        description: String,
        required: Boolean = false,
        default: Int? = null,
        minimum: Int? = null,
        maximum: Int? = null
    ) = SchemaProperty(
        type = "integer",
        description = description,
        required = required,
        default = default?.let { JsonPrimitive(it) },
        minimum = minimum,
        maximum = maximum
    )
    
    fun boolean(
        description: String,
        required: Boolean = false,
        default: Boolean? = null
    ) = SchemaProperty(
        type = "boolean",
        description = description,
        required = required,
        default = default?.let { JsonPrimitive(it) }
    )
    
    fun array(
        description: String,
        itemType: SchemaProperty,
        required: Boolean = false
    ) = SchemaProperty(
        type = "array",
        description = description,
        required = required,
        items = itemType
    )
    
    fun objectType(
        description: String,
        properties: Map<String, SchemaProperty>,
        required: Boolean = false,
        additionalProperties: Boolean = false
    ) = SchemaProperty(
        type = "object",
        description = description,
        required = required,
        properties = properties,
        additionalProperties = additionalProperties
    )
}
