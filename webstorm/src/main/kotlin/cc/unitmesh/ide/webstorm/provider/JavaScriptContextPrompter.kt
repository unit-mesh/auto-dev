package cc.unitmesh.ide.webstorm.provider

import cc.unitmesh.devti.provider.ContextPrompter

class JavaScriptContextPrompter : ContextPrompter() {
    private val addtionContext = ""

    override fun displayPrompt(): String {
        return "$action for the code:\n```${lang}$addtionContext\n$selectedText\n```"
    }

    override fun requestPrompt(): String {
        return "$action for the code:\n```${lang}$addtionContext\n$selectedText\n```"
    }
}
