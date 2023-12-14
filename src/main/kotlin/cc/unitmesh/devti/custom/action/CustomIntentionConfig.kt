package cc.unitmesh.devti.custom.action

import kotlinx.serialization.Serializable

@Serializable
class CustomIntentionConfig {
    var title: String = ""
    var autoInvoke: Boolean = false
    var matchRegex: String = ""
    var template: String = ""
    val isBuiltIn: Boolean = false
    val priority: Int = 0
}