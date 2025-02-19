package cc.unitmesh.dependencies

import cc.unitmesh.devti.provider.BuildSystemProvider
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.packageChecker.api.PackageDeclaration
import com.intellij.packageChecker.model.Dependency
import com.intellij.packageChecker.model.LibraryDependency
import com.intellij.packageChecker.model.impl.MutableModuleModel
import com.intellij.packageChecker.model.impl.ProjectDependenciesModelBase
import com.intellij.psi.PsiFile
import org.jetbrains.security.`package`.Package
import org.jetbrains.security.`package`.PackageType

class AutoDevProjectDependenciesModel(val project: Project) :
    ProjectDependenciesModelBase<MutableModuleModel>(AutoDevCoroutineScope.scope(project).coroutineContext) {
    override fun copyModule(
        module: MutableModuleModel,
        newDependencies: Iterable<Dependency>
    ): MutableModuleModel = module.withDependencies(newDependencies)

    override fun getAllModules(): Iterable<MutableModuleModel> = listOf()

    override fun declaredDependencies(module: Module): List<PackageDeclaration> = listOf()

    override fun declaredDependencies(psiFile: PsiFile): List<PackageDeclaration> {
        return BuildSystemProvider.EP_NAME.extensionList.map {
            it.collectDependencies(project, psiFile)
        }.flatten().map {
            val type = PackageType.fromString(it.type)
            val pkg = Package(type, it.namespace, it.name, it.version, it.qualifiers, it.subpath)
            PackageDeclaration(pkg)
        }
    }

    override fun libraryDependencies(module: Module): List<LibraryDependency> = listOf()

    override fun supports(module: Module): Boolean = true

    override fun supports(project: Project): Boolean = true

    override fun supports(psiFile: PsiFile): Boolean = true

}
