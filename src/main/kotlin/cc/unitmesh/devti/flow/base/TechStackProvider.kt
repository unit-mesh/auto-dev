package cc.unitmesh.devti.flow.base

import cc.unitmesh.devti.prompting.model.TestStack
import com.intellij.openapi.extensions.ExtensionPointName

interface TechStackProvider {
    fun prepareLibrary(): TestStack

    companion object {
        private val EP_NAME: ExtensionPointName<TechStackProvider> =
            ExtensionPointName.create("cc.unitmesh.techStackProvider")

        fun stack(): TechStackProvider? = EP_NAME.extensionList.firstOrNull()
    }
}
