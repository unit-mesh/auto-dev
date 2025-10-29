package cc.unitmesh.yaml

import com.charleskorn.kaml.Yaml

/**
 * Multiplatform YAML utilities using kaml library
 * Provides compatibility layer for snakeyaml migration
 */
object YamlUtils {

    private val yaml = Yaml.default

    /**
     * Parse YAML string to a Map<String, Any>
     * Compatible with snakeyaml's Yaml.load() method
     */
    fun load(yamlContent: String): Map<String, Any>? {
        if (yamlContent.isBlank()) return null

        return try {
            // Parse YAML to YamlNode first, then convert to Map
            val yamlNode = yaml.parseToYamlNode(yamlContent)
            yamlNodeToAny(yamlNode) as? Map<String, Any>
        } catch (e: Exception) {
            throw YamlParseException("Failed to parse YAML: ${e.message}", e)
        }
    }

    /**
     * Parse YAML string to a specific type T
     */
    fun <T> loadAs(yamlContent: String, serializer: kotlinx.serialization.KSerializer<T>): T {
        return try {
            yaml.decodeFromString(serializer, yamlContent)
        } catch (e: Exception) {
            throw YamlParseException("Failed to parse YAML: ${e.message}", e)
        }
    }

    /**
     * Convert object to YAML string
     */
    fun <T> dump(obj: T, serializer: kotlinx.serialization.KSerializer<T>): String {
        return try {
            yaml.encodeToString(serializer, obj)
        } catch (e: Exception) {
            throw YamlSerializationException("Failed to serialize to YAML: ${e.message}", e)
        }
    }
    
    /**
     * Convert YamlNode to Any (for compatibility with existing code)
     */
    private fun yamlNodeToAny(node: com.charleskorn.kaml.YamlNode): Any? {
        return when (node) {
            is com.charleskorn.kaml.YamlNull -> null
            is com.charleskorn.kaml.YamlScalar -> {
                val content = node.content
                when {
                    content == "true" -> true
                    content == "false" -> false
                    content.toIntOrNull() != null -> content.toInt()
                    content.toLongOrNull() != null -> content.toLong()
                    content.toDoubleOrNull() != null -> content.toDouble()
                    else -> content
                }
            }
            is com.charleskorn.kaml.YamlList -> node.items.map { yamlNodeToAny(it) }
            is com.charleskorn.kaml.YamlMap -> {
                val result = mutableMapOf<String, Any?>()
                for (entry in node.entries) {
                    val key = yamlNodeToAny(entry.key) as String
                    val value = yamlNodeToAny(entry.value)
                    result[key] = value
                }
                result
            }
            is com.charleskorn.kaml.YamlTaggedNode -> yamlNodeToAny(node.innerNode)
            else -> throw YamlParseException("Unsupported YAML node type: ${node::class.simpleName}")
        }
    }
}

/**
 * Exception thrown when YAML parsing fails
 */
class YamlParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown when YAML serialization fails
 */
class YamlSerializationException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Compatibility class that mimics snakeyaml's Yaml class
 */
class YamlCompat {
    /**
     * Load YAML content as Map<String, Any>
     * Compatible with snakeyaml's load method
     */
    fun <T> load(yamlContent: String): T? {
        @Suppress("UNCHECKED_CAST")
        return YamlUtils.load(yamlContent) as? T
    }
}
