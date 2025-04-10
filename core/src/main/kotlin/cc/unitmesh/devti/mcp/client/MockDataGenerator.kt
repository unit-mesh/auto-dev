package cc.unitmesh.devti.mcp.client

import io.modelcontextprotocol.spec.McpSchema
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonObject


object MockDataGenerator {
    fun generateMockData(input: McpSchema.JsonSchema): JsonObject {
        val result = buildJsonObject {
            input.properties.forEach { (key: String, value: Any) ->
                when (value) {
                    is String -> put(key, JsonPrimitive("mock_$key"))
                    is Number -> put(key, JsonPrimitive(42))
                    is Boolean -> put(key, JsonPrimitive(true))
                    is Array<*> -> put(key, JsonPrimitive("[]"))
                    is Object -> putJsonObject(key) {
                        put("id", JsonPrimitive(1))
                        put("name", JsonPrimitive("mock_$key"))
                    }
                }
            }
        }
        return result
    }
}
