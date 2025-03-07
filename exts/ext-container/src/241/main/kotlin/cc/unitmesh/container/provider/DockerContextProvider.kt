package cc.unitmesh.container.provider

import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import com.intellij.docker.DockerFileSearch
import com.intellij.docker.dockerFile.parser.psi.DockerFileFromCommand
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.PsiFileImpl

class DockerContextProvider : ChatContextProvider {
    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean =
        DockerFileSearch.getInstance().getDockerFiles(project).isNotEmpty()

    override fun collect(
        project: Project,
        creationContext: ChatCreationContext
    ): List<ChatContextItem> {
        val dockerFiles = DockerFileSearch.getInstance().getDockerFiles(project).mapNotNull {
            runReadAction { PsiManager.getInstance(project).findFile(it) }
        }

        if (dockerFiles.isEmpty()) return emptyList()

        var context = "This project use Docker."

        val virtualFile = dockerFiles.firstOrNull()?.virtualFile
            ?: return listOf(ChatContextItem(DockerContextProvider::class, context))

        context = "This project use Docker, path: ${virtualFile.path}"

        var additionalCtx = ""
        val fromCommands = runReadAction {
            dockerFiles.map {
                (it as PsiFileImpl).findChildrenByClass(DockerFileFromCommand::class.java).toList()
            }.flatten()
        }

        if (fromCommands.isEmpty()) return listOf(ChatContextItem(DockerContextProvider::class, context))
        additionalCtx = fromCommands.joinToString("\n") {
            runReadAction { it.text }
        }

        val text = "This project use Docker，Here is related Docker info: $additionalCtx"
        return listOf(ChatContextItem(DockerContextProvider::class, text))
    }
}