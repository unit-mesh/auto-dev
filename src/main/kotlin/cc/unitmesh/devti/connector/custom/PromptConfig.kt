package cc.unitmesh.devti.connector.custom

import kotlinx.serialization.*
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
@SerialName("Config")
data class PromptItem(val instruction: String, val input: String)

@Serializable(with = ConfigSerializer::class)
data class PromptConfig(val instruction: String, val input: String) {
    var key: String? = null
}

object ConfigSerializer : KSerializer<PromptConfig> {
    private val surrogateSerializer = PromptItem.serializer()
    private val delegateSerializer = MapSerializer(String.serializer(), surrogateSerializer)
    override val descriptor = delegateSerializer.descriptor

    override fun deserialize(decoder: Decoder): PromptConfig {
        val map = decoder.decodeSerializableValue(delegateSerializer)
        val (k, v) = map.entries.first()
        return PromptConfig(v.instruction, v.input).apply { key = k }
    }

    override fun serialize(encoder: Encoder, value: PromptConfig) {
        surrogateSerializer.serialize(encoder, PromptItem(value.instruction, value.input))
    }
}
