package cc.unitmesh.devti.connector.custom

import kotlinx.serialization.*
import kotlinx.serialization.json.*

//typealias CustomPromptConfig = HashMap<String, CustomPromptItem?>

@Serializable
data class PromptConfig(
    val fields: CustomPromptItem
)
@Serializable
data class CustomPromptItem(
    val instruction: String,
    val input: String,
)
