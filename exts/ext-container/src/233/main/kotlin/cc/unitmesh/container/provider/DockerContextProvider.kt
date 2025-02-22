package cc.unitmesh.container.provider

import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.devti.sketch.ui.patch.readText
import com.intellij.docker.DockerFileSearch
import com.intellij.docker.dockerFile.parser.psi.DockerFileFromCommand
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil

class DockerContextProvider : ChatContextProvider {
    private val fromRegex = Regex("FROM\\s+((?:--platform=[^\\s]+\\s+)?[^\\s]+)(?:\\s+AS\\s+([^\\s]+))?")

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
        try {
            val fromCommands = dockerFiles.mapNotNull {
                PsiTreeUtil.getChildrenOfType(it, DockerFileFromCommand::class.java)?.toList()
            }.flatten()

            if (fromCommands.isEmpty()) return listOf(ChatContextItem(DockerContextProvider::class, context))
            additionalCtx = fromCommands.joinToString("\n") {
                runReadAction { it.text }
            }
        } catch (e: Exception) {
            logger<DockerContextProvider>().warn("Failed to collect Docker context", e)
            val fromMatch = fromRegex.find(virtualFile.readText())

            if (fromMatch != null) {
                additionalCtx = fromMatch.groupValues[1]
            }

            if (additionalCtx.isEmpty()) {
                return listOf(ChatContextItem(DockerContextProvider::class, context))
            }
        }

        val text = "This project use Docker to run in server. Here is related info:\n$additionalCtx"
        return listOf(ChatContextItem(DockerContextProvider::class, text))
    }
}