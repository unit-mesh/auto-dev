package cc.unitmesh.devti.gui.chat


interface PromptFormatter {
    fun getUIPrompt(): String

    fun getRequestPrompt(): String
}

class ActionPromptFormatter(private val action: String, private val lang: String, private val selectedText: String) :
    PromptFormatter {
    override fun getUIPrompt(): String {
        var prompt = """$action this $lang code"""

        if (action == "explain") {
            prompt = "解释如下的 $lang 代码"
        }

        return """$prompt:
         <pre><code>$selectedText</pre></code>
        """.trimMargin()
    }

    override fun getRequestPrompt(): String {
        var prompt = """$action this $lang code"""

        if (action == "explain") {
            prompt = "解释如下的 $lang 代码"
        }

        return """$prompt:
            $selectedText
        """.trimMargin()
    }
}