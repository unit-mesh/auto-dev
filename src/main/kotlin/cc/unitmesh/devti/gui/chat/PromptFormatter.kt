package cc.unitmesh.devti.gui.chat


interface PromptFormatter {
    fun getUIPrompt(): String
    fun getRequestPrompt(): String
}

