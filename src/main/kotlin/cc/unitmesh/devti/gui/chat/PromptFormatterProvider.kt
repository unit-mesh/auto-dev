package cc.unitmesh.devti.gui.chat

import com.intellij.openapi.extensions.ExtensionPointName


interface PromptFormatterProvider {
    fun getUIPrompt(): String
    fun getRequestPrompt(): String

    companion object {
        private val EP_NAME: ExtensionPointName<PromptFormatterProvider> =
            ExtensionPointName.create("cc.unitmesh.prompterFormatterProvider")

        fun formatterProvider(): PromptFormatterProvider? = EP_NAME.extensionList.asSequence().firstOrNull()
    }

}

