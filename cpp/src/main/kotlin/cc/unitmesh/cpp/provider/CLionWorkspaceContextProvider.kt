package cc.unitmesh.cpp.provider

import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import com.intellij.execution.wsl.WslPath
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.cidr.project.workspace.CidrWorkspace
import java.io.File

class CLionWorkspaceContextProvider : ChatContextProvider {

    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        return creationContext.sourceFile?.language is OCLanguage
    }

    override suspend fun collect(project: Project, creationContext: ChatCreationContext): List<ChatContextItem> {
        val projectNameItem = createProjectNameItem(project)
        val configFileItem = createConfigFilesItem(project)
        val isUnderWslItem = createIsUnderWslItem(project)
        val preferredLanguageItem = createPreferredLanguageItem(project, creationContext)

        val testFrameworkItem = createTestFrameworkItem(project, creationContext)

        return (listOf(
            projectNameItem,
            configFileItem
        ) + isUnderWslItem + preferredLanguageItem + testFrameworkItem).filterNotNull()
    }

    private fun createProjectNameItem(project: Project): ChatContextItem {
        val text = "You are working on project named \"${project.name}\""
        return ChatContextItem(CLionWorkspaceContextProvider::class, text)
    }

    private fun createConfigFilesItem(project: Project): ChatContextItem {
        val configFiles = CMakefileUtil.collectConfigFiles(project)
        val configFileNames = configFiles.joinToString(", ") { it.name }
        return ChatContextItem(
            CLionWorkspaceContextProvider::class,
            "The project has the following config files: $configFileNames."
        )
    }

    private fun createIsUnderWslItem(project: Project): List<ChatContextItem> {
        val basePath = project.basePath ?: ""
        val isUnderWsl = WslPath.isWslUncPath(basePath)
        return if (isUnderWsl) {
            listOf(ChatContextItem(CLionWorkspaceContextProvider::class, "The project is opened under WSL."))
        } else {
            emptyList()
        }
    }

    private fun createTestFrameworkItem(project: Project, creationContext: ChatCreationContext): ChatContextItem? {
        val cmakeLists = File(project.basePath, "CMakeLists.txt")
        if (!cmakeLists.exists()) {
            logger<CLionWorkspaceContextProvider>().warn("CMakeLists.txt does not exist in the project.")
            return null
        }

        val text = cmakeLists.readText()
        if (text.contains("gtest") || text.contains("gmock")) {
            return ChatContextItem(
                CLionWorkspaceContextProvider::class,
                "The project uses Google Test framework."
            )
        }

        if (text.contains("catch") || text.contains("Catch")) {
            return ChatContextItem(
                CLionWorkspaceContextProvider::class,
                "The project uses Catch2 framework."
            )
        }

        return null
    }

    private fun createPreferredLanguageItem(
        project: Project,
        creationContext: ChatCreationContext
    ): List<ChatContextItem> {
        val sourceFile = creationContext.sourceFile
        return if (sourceFile != null) {
            val text =
                "Prefer ${sourceFile.language.displayName} language if the used language and toolset are not defined below or in the user messages."

            listOf(ChatContextItem(CLionWorkspaceContextProvider::class, text))
        } else {
            val initializedWorkspaces = CidrWorkspace.getInitializedWorkspaces(project)
            if (initializedWorkspaces.isEmpty()) {
                emptyList()
            } else {
                val text =
                    "Prefer C++ and C languages if the used language and toolset are not defined below or in the user messages."

                listOf(ChatContextItem(CLionWorkspaceContextProvider::class, text))
            }
        }
    }

}