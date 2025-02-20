package cc.unitmesh.container

import cc.unitmesh.devti.provider.RunService
import com.intellij.execution.configurations.RunProfile
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class RunContainerService : RunService {
    override fun isApplicable(project: Project, file: VirtualFile): Boolean {
        return false
    }

    override fun runConfigurationClass(project: Project): Class<out RunProfile>? {
        return null
    }
}
