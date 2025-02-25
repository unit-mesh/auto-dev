package cc.unitmesh.devti.bridge.provider

import cc.unitmesh.devti.bridge.tools.UiComponent
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.LazyExtensionInstance
import com.intellij.util.xmlb.annotations.Attribute

abstract class UiComponentProvider : LazyExtensionInstance<UiComponentProvider>() {
    @Attribute("implementationClass")
    var implementationClass: String? = null

    override fun getImplementationClassName(): String? = implementationClass

    open fun collect(project: Project): List<UiComponent> {
        return emptyList()
    }

    companion object {
        val EP_NAME: ExtensionPointName<UiComponentProvider> =
            ExtensionPointName.create("cc.unitmesh.uiComponentProvider")
    }

}