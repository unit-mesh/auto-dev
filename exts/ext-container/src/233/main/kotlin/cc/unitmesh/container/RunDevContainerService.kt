package cc.unitmesh.container

import cc.unitmesh.devti.provider.RunService
import com.intellij.docker.agent.util.nullize
import com.intellij.docker.connection.sshId
import com.intellij.docker.utils.createDefaultDockerServer
import com.intellij.docker.utils.getDockerServers
import com.intellij.execution.configurations.RunProfile
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.remoteServer.configuration.RemoteServer

class RunDevContainerService : RunService {
    override fun isApplicable(project: Project, file: VirtualFile): Boolean = file.name == "devcontainer.json"

    override fun runConfigurationClass(project: Project): Class<out RunProfile>? = null

    override fun runFile(
        project: Project,
        virtualFile: VirtualFile,
        psiElement: PsiElement?,
        isFromToolAction: Boolean
    ): String? {
        val createActions = createActions(project)
        if (createActions.isEmpty()) {
            return null
        }

        return createActions.first().templatePresentation.text
    }

    fun createActions(project: Project): List<AnAction> {
        val filteredServers =
            getDockerServers().filter {
                it.sshId == null
            }.nullize()
                ?: listOf(createDefaultDockerServer("Local"))

        return filteredServers.map {
            createDevcontainerCreateWithMountedSources(it)
        }
    }

    fun createDevcontainerCreateWithMountedSources(server: RemoteServer<*>): AnAction {
        val clazz = Class.forName("com.intellij.clouds.docker.gateway.actions.DevcontainerCreateWithMountedSources")
        val constructor = clazz.declaredConstructors.firstOrNull {
            it.parameterCount == 1 && it.parameterTypes[0] == RemoteServer::class.java
        } ?: throw IllegalStateException("Constructor not found")

        constructor.isAccessible = true
        return constructor.newInstance(server) as AnAction
    }
}
