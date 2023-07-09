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

                val isController = fileName.endsWith("Controller.java")
                val isService = fileName.endsWith("Service.java") || fileName.endsWith("ServiceImpl.java")

                when {
                    isController -> {
                        prompt = """代码补全 $lang 要求：
                                            |1. 在 Controller 中使用 BeanUtils 完成 DTO 的转换
                                            |2. 不允许把 json，map 这类对象传到 service 中
                                            |3. 不允许在 Controller 中使用 @Autowired
                                        """.trimMargin()
                    }

                    isService -> {
                        prompt = """代码补其 $lang 要求：
                                            |1. 不允许同时使用 BeanUtils 和 Map 转换 DTO
                                            |2. 直接调用 repository 的方法时，使用 get, find, count, delete, save, update 这类方法
                                            |3. Service 层应该捕获并处理可能出现的异常。通常情况下，应该将异常转换为应用程序自定义异常并抛出。
                                            """.trimMargin()
                    }
                }

            }

            else -> {}
        }

        return prompt
    }
}