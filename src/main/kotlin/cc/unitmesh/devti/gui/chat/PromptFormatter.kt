package cc.unitmesh.devti.gui.chat


interface PromptFormatter {
    fun getUIPrompt(): String

    fun getRequestPrompt(): String
}

class ActionPromptFormatter(private val action: String, private val lang: String, private val selectedText: String) :
    PromptFormatter {
    override fun getUIPrompt(): String {
        val prompt = """$action this $lang code"""

        return """$prompt:
         <pre><code>$selectedText</pre></code>
        """.trimMargin()
    }

    override fun getRequestPrompt(): String {
        val prompt = """$action this $lang code"""

        return """$prompt:
            $selectedText
        """.trimMargin()
    }
}