package cc.unitmesh.idea.provider

import cc.unitmesh.devti.pair.arch.ProjectPackageTree
import cc.unitmesh.devti.provider.architecture.LayeredArchProvider
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope

class JavaLayeredArchProvider : LayeredArchProvider {
    private val layeredArch = ProjectPackageTree()
    override fun isApplicable(project: Project): Boolean {
        val projectSdk = ProjectRootManager.getInstance(project).projectSdk ?: return false

        return projectSdk.sdkType is JavaSdkType
    }

    override fun getLayeredArch(project: Project): ProjectPackageTree {
        val searchScope: GlobalSearchScope = ProjectScope.getProjectScope(project)
        val javaFiles = FileTypeIndex.getFiles(JavaFileType.INSTANCE, searchScope)
        val psiManager = PsiManager.getInstance(project)

        javaFiles.forEach { javaFile ->
            val psiFile = psiManager.findFile(javaFile) ?: return@forEach
            val psiJavaFile = psiFile as? PsiJavaFile ?: return@forEach

            layeredArch.addPackage(psiJavaFile.packageName)
        }

        return layeredArch
    }
}
