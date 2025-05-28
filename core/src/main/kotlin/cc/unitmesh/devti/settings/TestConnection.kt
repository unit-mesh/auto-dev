package cc.unitmesh.devti.settings

import cc.unitmesh.cf.core.llms.LlmMsg
import cc.unitmesh.devti.fullWidthCell
import cc.unitmesh.devti.llm2.LLMProvider2
import cc.unitmesh.devti.llms.custom.Message
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.actionListener
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.swing.JButton
import javax.swing.JLabel

fun Panel.testLLMConnection() {
    row {
        // test result
        val result = JLabel("")

        val btn = cell(JButton("Test LLM Connection"))
        btn.actionListener { _, cmp ->
            val scope = CoroutineScope(CoroutineName("testConnection"))
            result.text = ""
            cmp.isEnabled = false

            // test custom engine
            scope.launch {
                try {
                    val llmProvider2 = LLMProvider2.GithubCopilot(modelName = "gpt-4")
                    val response = llmProvider2.request(Message("user","你是谁？"))
                    response.collectLatest {
                        result.text = it.chatMessage.content
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    result.text = e.message ?: "Unknown error"
                } finally {
                    cmp.isEnabled = true
                }
            }
        }

        fullWidthCell(result)
    }
}