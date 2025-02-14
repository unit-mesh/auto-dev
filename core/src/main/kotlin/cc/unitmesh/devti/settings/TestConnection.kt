package cc.unitmesh.devti.settings

import cc.unitmesh.cf.core.llms.LlmMsg
import cc.unitmesh.devti.fullWidthCell
import cc.unitmesh.devti.llm2.LLMProvider2
import cc.unitmesh.devti.llms.custom.Message
import com.intellij.ui.dsl.builder.Panel
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.swing.JLabel

fun Panel.testLLMConnection() {
    row {
        // test result
        val result = JLabel("")
        button("Test LLM Connection") {
            val scope = CoroutineScope(CoroutineName("testConnection"))
            result.text = ""
            result.isEnabled = false

            // test custom engine
            scope.launch {
                try {
                    val llmProvider2 = LLMProvider2()
                    val response = llmProvider2.request(Message("user","hi."))
                    response.collectLatest {
                        result.text = it.chatMessage.content
                    }
                } catch (e: Exception) {
                    result.text = e.message ?: "Unknown error"
                } finally {
                    result.isEnabled = true
                }
            }
        }

        fullWidthCell(result)
    }
}