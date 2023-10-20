package cc.unitmesh.devti.provider

import cc.unitmesh.devti.template.DockerfileContext
import com.intellij.openapi.project.Project

/**
 * The `BuildSystemProvider` interface represents a provider for build system information.
 * It provides methods to retrieve the name and version of the build tool being used, as well as the name and version of the programming language being used.
 *
 * This interface is used in conjunction with the [cc.unitmesh.devti.template.DockerfileContext] class.
 */
interface BuildSystemProvider {
    fun collect(project: Project): DockerfileContext
}