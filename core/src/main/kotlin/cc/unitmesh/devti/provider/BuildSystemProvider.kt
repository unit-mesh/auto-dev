package cc.unitmesh.devti.provider

import cc.unitmesh.devti.template.context.DockerfileContext
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.LazyExtensionInstance
import com.intellij.util.xmlb.annotations.Attribute

/**
 * The `BuildSystemProvider` interface represents a provider for build system information.
 * It provides methods to retrieve the name and version of the build tool being used, as well as the name and version of the programming language being used.
 *
 * This interface is used in conjunction with the [cc.unitmesh.devti.template.DockerfileContext] class.
 */
abstract class BuildSystemProvider : LazyExtensionInstance<BuildSystemProvider>() {
    abstract fun collect(project: Project): DockerfileContext?

    @Attribute("implementationClass")
    var implementationClass: String? = null

    override fun getImplementationClassName(): String? {
        return implementationClass
    }

    companion object {
        private val EP_NAME: ExtensionPointName<BuildSystemProvider> =
            ExtensionPointName.create("cc.unitmesh.buildSystemProvider")

        fun guess(project: Project): List<DockerfileContext> {
            return EP_NAME.extensionList.mapNotNull {
                it.collect(project)
            }
        }
    }
}