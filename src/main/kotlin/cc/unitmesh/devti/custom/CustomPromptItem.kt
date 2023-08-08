package cc.unitmesh.devti.custom

import kotlinx.serialization.Serializable

@Serializable
data class CustomPromptItem(val instruction: String, val input: String, val requirements: String = "")