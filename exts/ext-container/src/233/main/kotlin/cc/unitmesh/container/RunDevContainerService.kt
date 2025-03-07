package cc.unitmesh.container

import cc.unitmesh.devti.provider.RunService
import com.intellij.clouds.docker.gateway.DockerDevcontainerDeployContext
import com.intellij.clouds.docker.gateway.ui.DockerDeployView
import com.intellij.docker.DockerCloudConfiguration
import com.intellij.docker.agent.devcontainers.DevcontainerPaths
import com.intellij.docker.agent.devcontainers.buildStrategy.DevcontainerBuildStrategy.LocalBuildData
import com.intellij.docker.agent.util.nullize
import com.intellij.docker.connection.sshId
import com.intellij.docker.utils.createDefaultDockerServer
import com.intellij.docker.utils.getDockerServers
import com.intellij.execution.configurations.RunProfile
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.remoteServer.configuration.RemoteServer
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import com.jetbrains.rd.util.lifetime.Lifetime
import java.io.File
import javax.swing.JComponent

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

        val server = dockerServers().firstOrNull() ?: return null
        val projectDir = project.guessProjectDir()!!.toNioPath().toFile()
        /// write virtualFile to projectDir/devcontainer.json
        val devcontainerFile = File(projectDir, "devcontainer.json")
        devcontainerFile.writeText(virtualFile.contentsToByteArray().toString(Charsets.UTF_8))

        val content = createContext(devcontainerFile, projectDir, server)
        val wrapper = object : DialogWrapper(project) {
            override fun createCenterPanel(): JComponent? {
                val lifetime = Lifetime.Companion.Eternal
                val component = DockerDeployView(project, lifetime, content).component

                val contentPlanel = BorderLayoutPanel()
                component.setBorder(JBUI.Borders.empty())
                contentPlanel.add(component)
                contentPlanel.revalidate()
                contentPlanel.repaint()
                return contentPlanel
            }
        }

        wrapper.show()
        return createActions.first().templatePresentation.text
    }

    fun createActions(project: Project): List<AnAction> {
        val filteredServers =
            dockerServers()

        return filteredServers.map {
            createDevcontainerCreateWithMountedSources(it)
        }
    }

    private fun dockerServers(): List<RemoteServer<DockerCloudConfiguration>> {
        val filteredServers =
            getDockerServers().filter {
                it.sshId == null
            }.nullize()
                ?: listOf(createDefaultDockerServer("Local"))
        return filteredServers
    }

    fun createContext(
        modelFile: File,
        workingDir: File,
        dockerServer: RemoteServer<DockerCloudConfiguration>
    ): DockerDevcontainerDeployContext {
        val path = modelFile.toPath()
        val computeSourcesMountPath = DevcontainerPaths.computeSourcesMountPath(path)
        val sources = (computeSourcesMountPath?.toFile())

        val deployContext = DockerDevcontainerDeployContext()
        deployContext.config = dockerServer
        deployContext.buildFromLocalSources = LocalBuildData(workingDir, modelFile, sources, true)
        return deployContext
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
