package cc.unitmesh.ide.webstorm.provider

import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.provider.TechStackProvider

class JavaScriptContextPrompter : ContextPrompter() {
    override fun displayPrompt(): String {
        val techStacks = TechStackProvider.stack("javascript")
        val frameworks = techStacks?.prepareLibrary()?.coreFrameworks ?: emptyMap()

        val frameInfo = if (frameworks.isNotEmpty()) {
            "// frameworks: ${frameworks.keys.joinToString(", ")}"
        } else {
            ""
        }

        return "$action for the code:\n```${lang}$frameInfo\n$selectedText\n```"
    }

    override fun requestPrompt(): String {
        return "$action for the code:\n```${lang}\n$selectedText\n```"
    }
}
