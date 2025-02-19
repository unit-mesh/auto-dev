package cc.unitmesh.go.provider

import cc.unitmesh.devti.provider.BuildSystemProvider
import cc.unitmesh.devti.provider.DevPackage
import cc.unitmesh.devti.template.context.DockerfileContext
import com.goide.vgo.mod.psi.VgoModuleSpec
import com.goide.vgo.mod.psi.VgoRequireDirective
import com.goide.vgo.project.VgoDependency
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

class GoBuildSystemProvider : BuildSystemProvider() {
    override fun collect(project: Project): DockerfileContext? {
        return null
    }

    override fun collectDependencies(
        project: Project,
        psiFile: PsiFile
    ): List<DevPackage> {
        return getDependencies(psiFile)
            .mapNotNull { vgoModuleSpec -> toPackage(vgoModuleSpec) }
            .toList()
    }

    fun getDependencies(modFile: PsiFile): List<VgoModuleSpec> {
        return PsiTreeUtil.getChildrenOfTypeAsList(modFile, VgoRequireDirective::class.java)
            .flatMap { requireDirective ->
                PsiTreeUtil.getChildrenOfTypeAsList(requireDirective, VgoModuleSpec::class.java)
            }
    }

    fun toPackage(dependency: VgoDependency): DevPackage? {
        val version = dependency.version ?: return null
        val importPath = dependency.importPath
        val lowerCase = importPath.lowercase()
        return DevPackage("go", null, lowerCase, version)
    }

    fun toPackage(moduleSpec: VgoModuleSpec): DevPackage? {
        val version = moduleSpec.moduleVersion ?: return null
        val name = moduleSpec.identifier.text.lowercase()
        return DevPackage("go", null, name, version.text)
    }
}
