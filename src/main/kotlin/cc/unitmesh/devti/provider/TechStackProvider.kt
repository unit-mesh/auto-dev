package cc.unitmesh.devti.provider

import cc.unitmesh.devti.prompting.model.TestStack
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.serviceContainer.LazyExtensionInstance
import com.intellij.util.xmlb.annotations.Attribute

abstract class TechStackProvider : LazyExtensionInstance<TechStackProvider>() {
    @Attribute("language")
    var language: String? = null

    @Attribute("implementationClass")
    var implementationClass: String? = null

    override fun getImplementationClassName(): String? {
        return implementationClass
    }

    abstract fun prepareLibrary(): TestStack

    companion object {
        private val EP_NAME: ExtensionPointName<TechStackProvider> =
            ExtensionPointName.create("cc.unitmesh.techStackProvider")

        fun stack(lang: String): TechStackProvider? {
            val extensionList = EP_NAME.extensionList
            val providers = extensionList.filter {
                it.language?.lowercase() == lang.lowercase()
            }

            return if (providers.isEmpty()) {
                extensionList.first()
            } else {
                providers.first()
            }
        }
    }
}
