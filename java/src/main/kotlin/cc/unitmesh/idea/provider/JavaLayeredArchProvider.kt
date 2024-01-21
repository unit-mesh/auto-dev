package cc.unitmesh.idea.provider

import cc.unitmesh.devti.pair.arch.ProjectPackageTree
import cc.unitmesh.devti.provider.architecture.LayeredArchProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.JavaPsiFacade

class JavaLayeredArchProvider : LayeredArchProvider {
    private val layeredArch = ProjectPackageTree()
    override fun isApplicable(project: Project): Boolean {
        val projectSdk = ProjectRootManager.getInstance(project).projectSdk ?: return false

        return projectSdk.sdkType is JavaSdkType
    }

    override fun getLayeredArch(project: Project): ProjectPackageTree {
        val psiFacade = JavaPsiFacade.getInstance(project)

        val projectRootManager = ProjectRootManager.getInstance(project)
        val contentRoots = projectRootManager.contentRoots
        for (contentRoot in contentRoots) {
            // todo implement this
            val psiPackage = psiFacade.findPackage(contentRoot.url)
            if (psiPackage != null) {
                layeredArch.addPackage(psiPackage.qualifiedName)
            }
        }

        return layeredArch
    }
}
