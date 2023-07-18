package cc.unitmesh.devti.gui.chat


interface PromptFormatterProvider {
    fun getUIPrompt(): String
    fun getRequestPrompt(): String
}

