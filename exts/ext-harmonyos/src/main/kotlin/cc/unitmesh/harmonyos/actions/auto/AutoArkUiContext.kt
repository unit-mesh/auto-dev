package cc.unitmesh.harmonyos.actions.auto

import cc.unitmesh.devti.template.context.TemplateContext

data class AutoArkUiContext(
    val requirement: String,
    val layoutOverride: String,
    val componentOverride: String,
    val language: String,
    var elements: List<String> = emptyList(),
) : TemplateContext
