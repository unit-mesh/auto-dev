package cc.unitmesh.container

import cc.unitmesh.devti.provider.RunService
import com.intellij.execution.configurations.RunProfile
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement

class RunDockerfileService : RunService {
    override fun isApplicable(project: Project, file: VirtualFile): Boolean = false

    override fun runConfigurationClass(project: Project): Class<out RunProfile>? = null


    override fun runFile(project: Project, virtualFile: VirtualFile, psiElement: PsiElement?, isFromToolAction: Boolean)
            : String? {
        return null
    }
}