package cc.unitmesh.python.provider

import cc.unitmesh.devti.provider.BuildSystemProvider
import cc.unitmesh.devti.provider.DevPackage
import cc.unitmesh.devti.template.context.DockerfileContext
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.jetbrains.python.packaging.PyRequirement
import com.jetbrains.python.packaging.PyRequirementParser

class PythonBuildSystemProvider : BuildSystemProvider() {
    override fun collect(project: Project): DockerfileContext? = null

    override fun collectDependencies(project: Project, psiFile: PsiFile): List<DevPackage> {
        if (psiFile.language.id != "Python") return emptyList()

        val reqs: List<PyRequirement> = PyRequirementParser.fromFile(psiFile.virtualFile)

        return reqs.map {
            DevPackage(
                type = "pypi",
                name = it.name,
                version = it.versionSpecs.firstOrNull()?.version ?: ""
            )
        }
    }
}
