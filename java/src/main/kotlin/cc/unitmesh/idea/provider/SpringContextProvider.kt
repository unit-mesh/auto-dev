package cc.unitmesh.idea.provider

import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.prompting.code.TestStack
import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.idea.context.library.SpringLibrary
import com.intellij.openapi.externalSystem.model.project.LibraryData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.gradle.util.GradleConstants

open class SpringContextProvider : ChatContextProvider {
    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        return hasProjectLibraries(project) && creationContext.action != ChatActionType.CODE_COMPLETE
    }

    private fun hasProjectLibraries(project: Project): Boolean {
        Companion.prepareLibraryData(project)?.forEach {
            if (it.groupId?.contains("org.springframework") == true) {
                return true
            }
        }

        return false
    }

    override suspend  fun collect(project: Project, creationContext: ChatCreationContext): List<ChatContextItem> {
        val techStacks = prepareLibrary(project)

        if (techStacks.coreFrameworks().isEmpty() && techStacks.testFrameworks().isEmpty()) {
            return emptyList()
        }

        val fileName = creationContext.sourceFile?.name ?: ""

        fun isController() = fileName.endsWith("Controller.java") || fileName.endsWith("Controller.kt")
        fun isService() =
            fileName.endsWith("Service.java") || fileName.endsWith("ServiceImpl.java")
                    || fileName.endsWith("Service.kt") || fileName.endsWith("ServiceImpl.kt")

        when {
            isController() -> {
                return listOf(
                    ChatContextItem(
                        SpringContextProvider::class,
                        "You are working on a project that uses ${techStacks.coreFrameworks.keys.joinToString(",")} to build RESTful APIs."
                    )
                )
            }

            isService() -> {
                return listOf(
                    ChatContextItem(
                        SpringContextProvider::class,
                        "You are working on a project that uses ${techStacks.coreFrameworks.keys.joinToString(",")} to build business logic."
                    )
                )
            }
        }

        return listOf(
            ChatContextItem(
                SpringContextProvider::class,
                "You are working on a project that uses ${techStacks.coreFrameworks.keys.joinToString(",")} to build business logic."
            )
        )
    }

    private fun prepareLibrary(project: Project): TestStack {
        val libraryDataList = Companion.prepareLibraryData(project)

        val testStack = TestStack()
        var hasMatchSpringMvc = false
        var hasMatchSpringData = false

        libraryDataList?.forEach {
            val name = it.groupId + ":" + it.artifactId
            if (!hasMatchSpringMvc) {
                SpringLibrary.SPRING_MVC.forEach { entry ->
                    entry.coords.forEach { coord ->
                        if (name.contains(coord)) {
                            testStack.coreFrameworks.putIfAbsent(entry.shortText, true)
                            hasMatchSpringMvc = true
                        }
                    }
                }
            }

            if (!hasMatchSpringData) {
                SpringLibrary.SPRING_DATA.forEach { entry ->
                    entry.coords.forEach { coord ->
                        if (name.contains(coord)) {
                            testStack.coreFrameworks.putIfAbsent(entry.shortText, true)
                            hasMatchSpringData = true
                        }
                    }
                }
            }

            when {
                name.contains("org.springframework.boot:spring-boot-test") -> {
                    testStack.testFrameworks.putIfAbsent("Spring Boot Test", true)
                }

                name.contains("org.assertj:assertj-core") -> {
                    testStack.testFrameworks.putIfAbsent("AssertJ", true)
                }

                name.contains("org.junit.jupiter:junit-jupiter") -> {
                    testStack.testFrameworks.putIfAbsent("JUnit 5", true)
                }

                name.contains("org.mockito:mockito-core") -> {
                    testStack.testFrameworks.putIfAbsent("Mockito", true)
                }

                name.contains("com.h2database:h2") -> {
                    testStack.testFrameworks.putIfAbsent("H2", true)
                }
            }
        }

        return testStack
    }

    companion object {
        fun prepareLibraryData(project: Project): List<LibraryData>? {
            val basePath = project.basePath ?: return null
            val projectData = ProjectDataManager.getInstance().getExternalProjectData(
                project, GradleConstants.SYSTEM_ID, basePath
            )

            val libraryDataList = projectData?.externalProjectStructure?.children?.filter {
                it.data is LibraryData
            }?.map {
                it.data as LibraryData
            }

            return libraryDataList
        }
    }
}
