package cc.unitmesh.devti.prompting.model

import kotlinx.serialization.Serializable

@Serializable
data class CustomPromptItem(val instruction: String, val input: String, val requirements: String = "")