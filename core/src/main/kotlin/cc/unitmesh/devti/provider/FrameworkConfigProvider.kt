package cc.unitmesh.devti.provider

import cc.unitmesh.devti.util.relativePath
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

interface FrameworkConfigProvider {
    suspend fun collect(project: Project): List<VirtualFile>

    companion object {
        private val EP_NAME: ExtensionPointName<FrameworkConfigProvider> =
            ExtensionPointName("cc.unitmesh.frameworkConfigProvider")

        suspend fun collectAll(project: Project): List<String> {
            return EP_NAME.extensionList.flatMap { it.collect(project) }.map {
                it.relativePath(project)
            }
        }
    }

}
