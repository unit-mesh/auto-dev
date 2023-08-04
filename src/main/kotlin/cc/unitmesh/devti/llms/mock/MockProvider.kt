package cc.unitmesh.devti.llms.mock

import cc.unitmesh.devti.llms.CodeCopilotProvider
import com.intellij.openapi.components.Service

@Service(Service.Level.PROJECT)
class MockProvider: CodeCopilotProvider {
    override fun autoComment(text: String): String {
        return ""
    }

    override fun findBug(text: String): String {
        return ""
    }

    override fun prompt(promptText: String): String {
        return ""
    }
}