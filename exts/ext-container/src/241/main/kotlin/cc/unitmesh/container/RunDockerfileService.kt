package cc.unitmesh.container

import cc.unitmesh.devti.provider.RunService
import com.intellij.docker.DockerCloudConfiguration
import com.intellij.docker.DockerCloudType
import com.intellij.docker.DockerRunConfigurationCreator
import com.intellij.docker.DockerServerRuntimesManager
import com.intellij.docker.deploymentSource.DockerImageDeploymentSourceType
import com.intellij.docker.runtimes.DockerServerRuntime
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.remoteServer.ServerType
import com.intellij.remoteServer.configuration.RemoteServer
import com.intellij.remoteServer.configuration.RemoteServersManager
import kotlinx.coroutines.future.await

class RunDockerfileService : RunService {
    override fun isApplicable(project: Project, file: VirtualFile): Boolean {
        if (file.name == "Dockerfile") {
            return true
        }

        return runReadAction {
            PsiManager.getInstance(project).findFile(file)?.language?.displayName == "Dockerfile"
        }
    }

    override fun runConfigurationClass(project: Project): Class<out RunProfile>? = null

    fun remoteServerConfig() = object : RemoteServer<DockerCloudConfiguration> {
        override fun getName(): String = "DockerConnection"
        override fun getType(): ServerType<DockerCloudConfiguration> = DockerCloudType.getInstance()
        override fun getConfiguration(): DockerCloudConfiguration = DockerCloudConfiguration.createDefault()

        override fun setName(name: String?) {}
    }

    fun listAllDockerAccounts(): MutableList<RemoteServer<DockerCloudConfiguration>> {
        return RemoteServersManager.getInstance().getServers<DockerCloudConfiguration>(DockerCloudType.getInstance())
    }

    override fun createConfiguration(
        project: Project,
        virtualFile: VirtualFile
    ): RunConfiguration? {
        val imageType = DockerImageDeploymentSourceType.getInstance()
        val imageSource = imageType.singletonSource
        val creator = DockerRunConfigurationCreator(project)
        val cloudType = DockerCloudType.getInstance()
        val deploymentConfiguration = creator.createDeploymentConfigurationFromTemplate(imageType)
            ?: cloudType.createDeploymentConfigurator(project)
                .createDefaultConfiguration(imageSource)

        val server: RemoteServer<*> = remoteServerConfig()
        val createConfiguration = creator.createConfiguration(imageSource, deploymentConfiguration, server)
        return createConfiguration.configuration
    }

    private suspend fun createRuntime(project: Project): DockerServerRuntime {
        val instance = DockerServerRuntimesManager.getInstance(project)
        val dockerConnection = remoteServerConfig()
        val runtime: DockerServerRuntime = instance
            .getOrCreateConnection(dockerConnection)
            .await()

        return runtime
    }
}