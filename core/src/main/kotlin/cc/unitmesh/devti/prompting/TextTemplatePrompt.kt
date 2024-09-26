package cc.unitmesh.devti.prompting

import cc.unitmesh.devti.template.TemplateRender
import cc.unitmesh.devti.template.context.TemplateContext

class TextTemplatePrompt(
    /**
     * The text to display to the user
     */
    var displayText: String,
    /**
     * The text request to the server
     */
    var requestText: String,

    /**
     * The template render to use
     */
    val templateRender: TemplateRender? = null,

    /**
     * The context to use for the template
     */
    val context: TemplateContext? = null
) {
    fun renderTemplate(): TextTemplatePrompt {
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