package cc.unitmesh.devti.settings

import cc.unitmesh.devti.fullWidthCell
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Panel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.swing.JLabel

fun Panel.testLLMConnection(project: Project?) {
    row {
        // test result
        val result = JLabel("")
        button("Test LLM Connection") {
            if (project == null) return@button
            result.text = ""

            // test custom engine
            AutoDevCoroutineScope.scope(project).launch {
                try {
                    val flowString: Flow<String> = LlmFactory.instance.create(project).stream("hi", "", false)
                    flowString.collect {
                        result.text += it
                    }
                } catch (e: Exception) {
                    result.text = e.message ?: "Unknown error"
                }
            }
        }

        fullWidthCell(result)
    }
}