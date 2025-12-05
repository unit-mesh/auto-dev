package cc.unitmesh.go.provider

import cc.unitmesh.devti.provider.BuildSystemProvider
import cc.unitmesh.devti.provider.DevPackage
import cc.unitmesh.devti.template.context.DockerfileContext
import com.goide.vgo.mod.psi.VgoModuleSpec
import com.goide.vgo.mod.psi.VgoRequireDirective
import com.goide.vgo.project.VgoDependency
import com.goide.vgo.project.VgoModulesRegistry
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

class GoBuildSystemProvider : BuildSystemProvider() {
    override fun collect(project: Project): DockerfileContext? {
        return null
    }

    override fun isDeclarePackageFile(filename: String): Boolean {
        return filename == "go.mod"
    }

    override fun collectDependencies(
        project: Project,
        buildFilePsi: PsiFile
    ): List<DevPackage> {
        return getDependencies(buildFilePsi)
            .mapNotNull { vgoModuleSpec -> toPackage(vgoModuleSpec) }
            .toList()
    }

    fun getAllModules(project: Project): Iterable<DevModuleModel> {
        val result = mutableListOf<DevModuleModel>()
        ModuleManager.getInstance(project).modules.forEach { module ->
            ProgressManager.checkCanceled()
            VgoModulesRegistry.getInstance(project).getModules(module).forEach { vgoModule ->
                val dependencies = vgoModule.dependencies.mapNotNull { vgoDependency ->
                    toPackage(vgoDependency)
                }.toSet()

                val importPath = vgoModule.importPath
                val buildFile = vgoModule.root.findChild("go.mod")
                val moduleModel = DevModuleModel(importPath, module, buildFile, dependencies, project)
                result.add(moduleModel)
            }
        }

        return result
    }

    data class DevModuleModel(
        val id: String,
        val platformModule: com.intellij.openapi.module.Module?,
        val buildFile: VirtualFile?,
        var dependencies: Set<DevPackage>,
        val project: Project,
        val dataContext: DataContext? = null,
        val parentModuleName: String? = null
    )

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
