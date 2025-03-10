package cc.unitmesh.devti.provider

class TextContextPrompter(val prompt: String) : ContextPrompter() {
    override fun displayPrompt(): String = prompt
    override fun requestPrompt(): String = prompt
}