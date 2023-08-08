package cc.unitmesh.devti.custom

import kotlinx.serialization.Serializable

@Serializable
class CustomIntentionPrompt {
    var title: String = ""
    var autoInvoke: Boolean = false
    var matchRegex: String = ""
    var template: String = ""
    val isBuiltIn: Boolean = false
}