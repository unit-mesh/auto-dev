package cc.unitmesh.container.provider

import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import com.intellij.docker.DockerFileSearch
import com.intellij.docker.dockerFile.parser.psi.DockerFileFromCommand
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil

class DockerContextProvider : ChatContextProvider {
    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean =
        DockerFileSearch.getInstance().getDockerFiles(project).isNotEmpty()

    override suspend fun collect(
        project: Project,
        creationContext: ChatCreationContext
    ): List<ChatContextItem> {

        val dockerFiles = DockerFileSearch.getInstance().getDockerFiles(project).mapNotNull {
            PsiManager.getInstance(project).findFile(it)
        }

        if (dockerFiles.isEmpty()) return emptyList()

        var context = "This project use Docker."

        try {
            val virtualFile = dockerFiles.firstOrNull()?.virtualFile
            context = "This project use Docker, path: ${virtualFile?.path}"

            val fromCommands = dockerFiles.mapNotNull {
                PsiTreeUtil.getChildrenOfType(it, DockerFileFromCommand::class.java)?.toList()
            }.flatten()

            if (fromCommands.isEmpty()) {
                return listOf(ChatContextItem(DockerContextProvider::class, context))
            }

            val text = "This project use Docker to run in server. Here is related info:\n" +
                    fromCommands.joinToString("\n") { it.text }

            return listOf(ChatContextItem(DockerContextProvider::class, text))
        } catch (e: Exception) {
            logger<DockerContextProvider>().warn("Failed to collect Docker context", e)
            return listOf(ChatContextItem(DockerContextProvider::class, context))
        }
    }
}
