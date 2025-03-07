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
        val server = dockerServers().firstOrNull() ?: return null
        val projectDir = project.guessProjectDir()!!.toNioPath().toFile()

        val devcontainerFile = File(projectDir, "devcontainer.json")
        devcontainerFile.writeText(virtualFile.contentsToByteArray().toString(Charsets.UTF_8))

        val content = createContext(devcontainerFile, projectDir, server)
        val wrapper = object : DialogWrapper(project) {
            override fun createCenterPanel(): JComponent? = BorderLayoutPanel()


            override fun beforeShowCallback() {
                val panel = contentPanel
                val lifetime = Lifetime.Companion.Eternal

                val dockerDeployView = DockerDeployView(project, lifetime, content)
                val component = dockerDeployView.component
                component.setBorder(JBUI.Borders.empty())

                panel.add(component)
                panel.revalidate()
                panel.repaint()
            }
        }

        wrapper.show()
        return "Running devcontainer.json"
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
        val buildData = createBuildData(workingDir, modelFile, sources)
        deployContext.buildFromLocalSources = buildData
        return deployContext
    }


    private fun createBuildData(
        workingDir: File,
        modelFile: File,
        sources: File?
    ): LocalBuildData {
        return try {
            val newConstructor: java.lang.reflect.Constructor<LocalBuildData> = LocalBuildData::class.java.getConstructor(
                File::class.java,
                File::class.java,
                Boolean::class.javaPrimitiveType
            )
            newConstructor.newInstance(modelFile, sources ?: workingDir, true)
        } catch (e: NoSuchMethodException) {
            val oldConstructor: java.lang.reflect.Constructor<LocalBuildData> = LocalBuildData::class.java.getConstructor(
                File::class.java,
                File::class.java,
                File::class.java,
                Boolean::class.javaPrimitiveType
            )
            oldConstructor.newInstance(workingDir, modelFile, sources, true)
        }
    }
}
