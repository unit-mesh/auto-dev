/*
 * MIT License
 *
 * Copyright (c) 2025 https://github.com/aallam/openai-kotlin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cc.unitmesh.devti.llms.call

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

@JvmInline
@Serializable
public value class ToolType(public val value: String) {
    public companion object {
        public val Function: ToolType = ToolType("function")
    }
}

@Serializable
data class FunctionToolTool(
    @SerialName("type") val type: ToolType,
    @SerialName("function") val function: FunctionTool,
)

@Serializable
public data class FunctionTool(
    @SerialName("name") val name: String,
    @SerialName("parameters") val parameters: Parameters? = null,
    @SerialName("description") public val description: String? = null
)

@Serializable(with = Parameters.JsonDataSerializer::class)
public data class Parameters(public val schema: JsonElement) {
    public object JsonDataSerializer : KSerializer<Parameters> {
        override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

        override fun deserialize(decoder: Decoder): Parameters {
            require(decoder is JsonDecoder) { "This decoder is not a JsonDecoder. Cannot deserialize `FunctionParameters`." }
            return Parameters(decoder.decodeJsonElement())
        }

        override fun serialize(encoder: Encoder, value: Parameters) {
            require(encoder is JsonEncoder) { "This encoder is not a JsonEncoder. Cannot serialize `FunctionParameters`." }
            encoder.encodeJsonElement(value.schema)
        }
    }

    public companion object {
        public fun fromJsonString(json: String): Parameters = Parameters(Json.parseToJsonElement(json))

        public fun buildJsonObject(block: JsonObjectBuilder.() -> Unit): Parameters {
            val json = kotlinx.serialization.json.buildJsonObject(block)
            return Parameters(json)
        }

        public val Empty: Parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {}
        }
    }
}