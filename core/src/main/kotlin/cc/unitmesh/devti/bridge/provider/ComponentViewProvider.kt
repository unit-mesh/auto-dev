package cc.unitmesh.devti.bridge.provider

import cc.unitmesh.devti.bridge.archview.model.UiComponent
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.LazyExtensionInstance
import com.intellij.util.xmlb.annotations.Attribute

abstract class ComponentViewProvider : LazyExtensionInstance<ComponentViewProvider>() {
    @Attribute("implementationClass")
    var implementationClass: String? = null

    override fun getImplementationClassName(): String? = implementationClass

    abstract fun isApplicable(project: Project): Boolean

    abstract fun collect(project: Project): List<UiComponent>

    companion object {
        val EP_NAME: ExtensionPointName<ComponentViewProvider> =
            ExtensionPointName.create("cc.unitmesh.componentProvider")

        fun collect(project: Project): List<UiComponent> {
            return EP_NAME.extensionList
                .filter { it.isApplicable(project) }
                .flatMap {
                    it.collect(project)
                }
        }
    }
}