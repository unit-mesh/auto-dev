package cc.unitmesh.devti.mcp.client

import io.modelcontextprotocol.kotlin.sdk.Tool.Input
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.putJsonObject

object MockDataGenerator {
    fun generateMockData(input: Input): JsonObject {
        val result = buildJsonObject {
            input.properties.forEach { (key: String, value: JsonElement) ->
                val type = value.jsonObject["type"]?.jsonPrimitive?.content
                when (type) {
                    "string" -> put(key, JsonPrimitive("mock_$key"))
                    "number" -> put(key, JsonPrimitive(42))
                    "integer" -> put(key, JsonPrimitive(42))
                    "boolean" -> put(key, JsonPrimitive(true))
                    "array" -> put(key, JsonPrimitive("[]"))
                    "object" -> putJsonObject(key) {
                        put("id", JsonPrimitive(1))
                        put("name", JsonPrimitive("mock_$key"))
                    }
                }
            }
        }
        return result
    }
} 