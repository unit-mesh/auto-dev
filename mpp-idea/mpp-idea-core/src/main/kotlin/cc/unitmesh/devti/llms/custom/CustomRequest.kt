package cc.unitmesh.devti.llms.custom

import com.intellij.openapi.diagnostic.logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.Request
import org.jetbrains.annotations.VisibleForTesting

@Serializable
data class CustomRequest(val messages: List<Message>)

@VisibleForTesting
fun Request.Builder.appendCustomHeaders(customRequestHeader: String): Request.Builder = apply {
    runCatching {
        Json.Default.parseToJsonElement(customRequestHeader)
            .jsonObject["customHeaders"].let { customFields ->
            customFields?.jsonObject?.forEach { (key, value) ->
                header(key, value.jsonPrimitive.content)
            }
        }
    }.onFailure {
        logger<CustomLLMProvider>().warn("Failed to parse custom request header", it)
    }
}

@VisibleForTesting
fun JsonObject.updateCustomBody(customRequest: String): JsonObject {
    return runCatching {
        buildJsonObject {
            // copy origin object
            val customRequestJson = Json.Default.parseToJsonElement(customRequest).jsonObject
            customRequestJson["fields"]?.jsonObject?.let { fieldsObj ->
                val messages: JsonArray = this@updateCustomBody["messages"]?.jsonArray ?: buildJsonArray {}
                val contentOfFirstMessage = if (messages.isNotEmpty()) {
                    messages.last().jsonObject["content"]?.jsonPrimitive?.content ?: ""
                } else ""
                fieldsObj.forEach { (fieldKey, fieldValue) ->
                    if (fieldValue is JsonObject) {
                        put(fieldKey, buildJsonObject {
                            fieldValue.forEach { (subKey, subValue) ->
                                if (subValue is JsonPrimitive && subValue.content == "\$content") {
                                    put(subKey, JsonPrimitive(contentOfFirstMessage))
                                } else {
                                    put(subKey, subValue)
                                }
                            }
                        })
                    } else if (fieldValue is JsonPrimitive && fieldValue.content == "\$content") {
                        put(fieldKey, JsonPrimitive(contentOfFirstMessage))
                    } else {
                        put(fieldKey, fieldValue)
                    }
                }

                return@buildJsonObject
            }

            this@updateCustomBody.forEach { u, v -> put(u, v) }
            customRequestJson["customFields"]?.let { customFields ->
                customFields.jsonObject.forEach { (key, value) ->
                    put(key, value)
                }
            }

            // TODO clean code with magic literals
            var roleKey = "role"
            var contentKey = "content"
            customRequestJson.jsonObject["messageKeys"]?.let {
                roleKey = it.jsonObject["role"]?.jsonPrimitive?.content ?: "role"
                contentKey = it.jsonObject["content"]?.jsonPrimitive?.content ?: "content"
            }

            val messages: JsonArray = this@updateCustomBody["messages"]?.jsonArray ?: buildJsonArray { }
            this.put("messages", buildJsonArray {
                messages.forEach { message ->
                    val role: String = message.jsonObject["role"]?.jsonPrimitive?.content ?: "user"
                    val content: String = message.jsonObject["content"]?.jsonPrimitive?.content ?: ""
                    add(buildJsonObject {
                        put(roleKey, role)
                        put(contentKey, content)
                    })
                }
            })
        }
    }.getOrElse {
        logger<CustomLLMProvider>().error("Failed to parse custom request body", it)
        this
    }
}

fun CustomRequest.updateCustomFormat(format: String): String {
    val requestContentOri = Json.Default.encodeToString<CustomRequest>(this)
    val updateCustomBody = runCatching {
        Json.Default.parseToJsonElement(requestContentOri)
            .jsonObject.updateCustomBody(format)
    }.getOrElse {
        logger<CustomLLMProvider>().error("Failed to update custom request body: ${format}", it)
        requestContentOri
    }

    return updateCustomBody.toString()
}
