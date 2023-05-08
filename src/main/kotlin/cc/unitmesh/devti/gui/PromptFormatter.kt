package cc.unitmesh.devti.gui


interface PromptFormatter {
    fun getUIPrompt(): String

    fun getRequestPrompt(): String
}

class ActionPromptFormatter(private val action: String, private val lang: String, private val selectedText: String) :
    PromptFormatter {
    override fun getUIPrompt(): String {
        return """$action this $lang code:
         <pre><code>$selectedText</pre></code>
        """.trimMargin()
    }

    override fun getRequestPrompt(): String {
        return """$action this $lang code:
            $selectedText
        """.trimMargin()
    }
}