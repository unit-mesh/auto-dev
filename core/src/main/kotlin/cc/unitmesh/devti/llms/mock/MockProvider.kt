package cc.unitmesh.devti.llms.mock

import cc.unitmesh.devti.llms.LLMProvider
import com.intellij.openapi.components.Service

@Service(Service.Level.PROJECT)
class MockProvider: LLMProvider {

    override fun prompt(promptText: String): String {
        return ""
    }
}