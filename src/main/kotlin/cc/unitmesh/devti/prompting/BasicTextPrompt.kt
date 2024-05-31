package cc.unitmesh.devti.prompting

import cc.unitmesh.devti.template.TemplateRender
import cc.unitmesh.devti.template.context.TemplateContext

class BasicTextPrompt(
    /**
     * The text to display to the user
     */
    var displayText: String,
    /**
     * The text request to the server
     */
    var requestText: String,

    val templateRender: TemplateRender? = null,

    val context: TemplateContext? = null
) {
    fun renderTemplate(): BasicTextPrompt {
        if (templateRender != null) {
            if (context != null) {
                templateRender.context = context
            }

            displayText = templateRender.renderTemplate(displayText)
            requestText = templateRender.renderTemplate(requestText)
        }

        return this
    }
}