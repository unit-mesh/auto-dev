package cc.unitmesh.devti.llms.azure

import kotlinx.serialization.Serializable

@Serializable
data class SimpleOpenAIBody(val messages: List<SimpleOpenAIFormat>, val temperature: Double, val stream: Boolean)