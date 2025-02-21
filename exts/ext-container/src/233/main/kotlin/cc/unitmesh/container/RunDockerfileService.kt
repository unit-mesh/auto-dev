package cc.unitmesh.container

import cc.unitmesh.devti.provider.RunService
import com.intellij.docker.dockerFile.DockerLanguage
import com.intellij.execution.configurations.RunProfile
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement

class RunDockerfileService : RunService {
    override fun isApplicable(project: Project, file: VirtualFile): Boolean = file.name == "Dockerfile"

    fun isApplicable(element: PsiElement) = element.containingFile?.language == DockerLanguage.INSTANCE

    override fun runConfigurationClass(project: Project): Class<out RunProfile>? {
        return null
    }
}
