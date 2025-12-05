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

    override fun isDeclarePackageFile(filename: String): Boolean {
        return filename == "requirements.txt" || filename == "Pipfile" || filename == "pyproject.toml"
    }

    override fun collectDependencies(project: Project, buildFilePsi: PsiFile): List<DevPackage> {
        val reqs: List<PyRequirement> = PyRequirementParser.fromFile(buildFilePsi.virtualFile)

        return reqs.map {
            DevPackage(
                type = "pypi",
                name = it.name,
                version = it.versionSpecs.firstOrNull()?.version ?: ""
            )
        }
    }
}
