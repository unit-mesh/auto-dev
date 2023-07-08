package cc.unitmesh.devti.gui.chat

import com.intellij.openapi.util.NlsSafe


interface PromptFormatter {
    fun getUIPrompt(): String

    fun getRequestPrompt(): String
}

class ActionPromptFormatter(
    private val action: ChatBotActionType,
    private val lang: String,
    private val selectedText: String,
    private val fileName: @NlsSafe String,
) : PromptFormatter {

    override fun getUIPrompt(): String {
        val prompt = createPrompt()

        return """$prompt:
         <pre><code>$selectedText</pre></code>
        """.trimMargin()
    }

    override fun getRequestPrompt(): String {
        val prompt = createPrompt()

        return """$prompt:
            $selectedText
        """.trimMargin()
    }

    private fun createPrompt(): String {
        var prompt = """$action this $lang code"""

        when (action) {
            ChatBotActionType.REVIEW -> {
                prompt = "检查如下的 $lang 代码"
            }

            ChatBotActionType.EXPLAIN -> {
                prompt = "解释如下的 $lang 代码"
            }

            else -> {}
        }

        return prompt
    }
}