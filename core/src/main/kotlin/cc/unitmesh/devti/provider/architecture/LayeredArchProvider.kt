package cc.unitmesh.devti.provider.architecture

import cc.unitmesh.devti.pair.arch.ProjectPackageTree
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

interface LayeredArchProvider {
    fun isApplicable(project: Project): Boolean
    fun getLayeredArch(project: Project): ProjectPackageTree

    companion object {
        private val EP_NAME = ExtensionPointName<LayeredArchProvider>("cc.unitmesh.layeredArchProvider")

        fun find(project: Project): LayeredArchProvider? {
            val providers = EP_NAME.extensionList.filter {
                it.isApplicable(project)
            }

            return providers.firstOrNull()
        }
    }
}

