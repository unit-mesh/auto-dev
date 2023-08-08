package cc.unitmesh.devti.custom

import cc.unitmesh.devti.provider.context.ChatContextItem

class CustomIntentionPrompt(
    val displayPrompt: String,
    val requestPrompt: String,
    val contextItems: List<ChatContextItem> = listOf()
)