package cc.unitmesh.container

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.provider.RunService
import com.intellij.clouds.docker.gateway.DockerDevcontainerDeployContext
import com.intellij.docker.DockerCloudConfiguration
import com.intellij.docker.agent.devcontainers.DevcontainerPaths
import com.intellij.docker.agent.devcontainers.buildStrategy.DevcontainerBuildStrategy.LocalBuildData
import com.intellij.docker.agent.util.nullize
import com.intellij.docker.connection.sshId
import com.intellij.docker.utils.createDefaultDockerServer
import com.intellij.docker.utils.getDockerServers
import com.intellij.execution.configurations.RunProfile
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.remoteDev.hostStatus.UnattendedHostStatus
import com.intellij.remoteServer.configuration.RemoteServer
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import com.jetbrains.gateway.api.GatewayConnectionHandle
import com.jetbrains.gateway.api.GatewayConnector
import com.jetbrains.rd.util.lifetime.Lifetime
import java.io.File
import java.nio.file.Path
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
        val server = dockerServers().firstOrNull()
        if (server == null) {
            AutoDevNotifications.warn(project, "Cannot create DockerCloud server")
            return null
        }
        val projectDir = project.guessProjectDir()!!.toNioPath().toFile()

        val containerFile = File(projectDir, "devcontainer.json")
        containerFile.writeText(virtualFile.contentsToByteArray().toString(Charsets.UTF_8))

        val context = try {
            createContext(containerFile, projectDir, server)
        } catch (e: Exception) {
            logger<RunDevContainerService>().warn("Cannot create context: $e")
            DockerDevcontainerDeployContext()
        }

        val wrapper = object : DialogWrapper(project) {
            override fun createCenterPanel(): JComponent? = BorderLayoutPanel()
            override fun beforeShowCallback() {
                val panel = contentPanel
                val lifetime = Lifetime.Companion.Eternal

                val baseLine = ApplicationInfo.getInstance().build.baselineVersion
                val component = try {
                    if (baseLine >= 242) {
                        createByLifetime(lifetime) ?: createDeployViewComponentFor242(project, lifetime, context)
                    } else {
                        createDeployViewComponentFor241(project, lifetime, context)
                    }
                } catch (e: Exception) {
                    logger<RunDevContainerService>().warn("Cannot create component: $e")
                    createByLifetime(lifetime) ?: throw e
                }

                component.setBorder(JBUI.Borders.empty())

                panel.add(component)
                panel.revalidate()
                panel.repaint()

            }
        }

        wrapper.show()
        return "Running devcontainer.json"
    }

    private fun createDeployViewComponentFor241(
        project: Project,
        lifetime: Lifetime,
        context: DockerDevcontainerDeployContext
    ): JComponent {
        val dockerDeployViewClass = Class.forName("com.intellij.clouds.docker.gateway.ui.DockerDeployView")
        val constructor = dockerDeployViewClass.getDeclaredConstructor(
            Project::class.java,
            Lifetime::class.java,
            DockerDevcontainerDeployContext::class.java,
            Function0::class.java,
            Function0::class.java
        )
        constructor.isAccessible = true

        val emptyAction = { }
        val dockerDeployViewInstance = constructor.newInstance(project, lifetime, context, emptyAction, emptyAction)

        val componentMethod = dockerDeployViewClass.getMethod("getComponent")
        val component = componentMethod.invoke(dockerDeployViewInstance) as JComponent
        return component
    }

    private fun createDeployViewComponentFor242(
        project: Project,
        lifetime: Lifetime,
        context: DockerDevcontainerDeployContext
    ): JComponent {
        val dockerDeployViewClass = Class.forName("com.intellij.clouds.docker.gateway.ui.DockerDeployView")
        val constructor = dockerDeployViewClass.getDeclaredConstructor(
            Project::class.java,
            Lifetime::class.java,
            DockerDevcontainerDeployContext::class.java,
            Function2::class.java
        )

        val function2 = { _: GatewayConnectionHandle, _: UnattendedHostStatus -> Unit }

        val instance = constructor.newInstance(project, lifetime, context, function2)
        val componentMethod = dockerDeployViewClass.getMethod("getComponent")
        val component = componentMethod.invoke(instance) as JComponent
        return component
    }

    fun createByLifetime(lifetime: Lifetime): JComponent? {
        val view = GatewayConnector.getConnectors()
            .firstOrNull { it.isAvailable() && it.javaClass.name == "com.intellij.clouds.docker.gateway.DockerGatewayConnector" }
            ?: return null

        val connectorView = view.createView(lifetime)

        return connectorView.component
    }

    private fun dockerServers(): List<RemoteServer<DockerCloudConfiguration>> {
        val filteredServers =
            getDockerServers().filter {
                it.sshId == null
            }.nullize() ?: listOf(createDefaultDockerServer("Local"))
        return filteredServers
    }

    fun createContext(
        modelFile: File,
        workingDir: File,
        dockerServer: RemoteServer<DockerCloudConfiguration>
    ): DockerDevcontainerDeployContext {
        val path = modelFile.toPath()
        val sources = DevcontainerPaths.computeSourcesMountPath(path)?.toFile()

        logger<RunDevContainerService>().info("Creating context for $modelFile, $workingDir, $sources")

        val deployContext = DockerDevcontainerDeployContext()
        deployContext.config = dockerServer
        deployContext.buildFromLocalSources = createBuildData(workingDir, modelFile, sources)
        return deployContext
    }

    private fun createBuildData(
        workingDir: File,
        modelFile: File,
        sources: File?
    ): LocalBuildData {
        return try {
            val oldConstructor: java.lang.reflect.Constructor<LocalBuildData> =
                LocalBuildData::class.java.getConstructor(
                    File::class.java,
                    File::class.java,
                    File::class.java,
                    Boolean::class.javaPrimitiveType
                )
            oldConstructor.newInstance(workingDir, modelFile, sources, true)
        } catch (e: NoSuchMethodException) {
            val newConstructor: java.lang.reflect.Constructor<LocalBuildData> =
                LocalBuildData::class.java.getConstructor(
                    Path::class.java,
                    Path::class.java,
                    Boolean::class.javaPrimitiveType
                )
            newConstructor.newInstance(modelFile.toPath(), (sources ?: workingDir).toPath(), true)
        }
    }
}
