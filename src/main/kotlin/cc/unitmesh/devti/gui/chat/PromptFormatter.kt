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

            ChatBotActionType.CODE_COMPLETE -> {
                prompt = "补全如下的 $lang 代码"

                if (fileName.endsWith("Controller.java")) {
                    prompt = """代码补全 $lang 要求：
                                |1. 在 Controller 中使用 BeanUtils 完成 DTO 的转换
                                |2. 不允许把 json，map 这类对象传到 service 中
                                |3. 不允许在 Controller 中使用 @Autowired
                            """.trimMargin()
                }

            }

            else -> {}
        }

        return prompt
    }
}