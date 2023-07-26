package cc.unitmesh.idea.java.provider

import cc.unitmesh.devti.prompting.code.TestStack
import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.model.project.LibraryData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.psi.PsiJavaFile
import org.jetbrains.plugins.gradle.util.GradleConstants

open class JavaTechStackContextProvider : ChatContextProvider {
    open val fileExt = "java"

    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        val psiFile = creationContext.sourceFile ?: return false
        return psiFile is PsiJavaFile
    }

    override fun collect(project: Project, creationContext: ChatCreationContext): List<ChatContextItem> {
        val techStacks = prepareLibrary()

        if (techStacks.coreFrameworks().isEmpty() && techStacks.testFrameworks().isEmpty()) {
            return emptyList()
        }

        val fileName = creationContext.sourceFile?.name ?: ""

        fun isController() = fileName.endsWith("Controller.$fileExt")
        fun isService() =
            fileName.endsWith("Service.$fileExt") || fileName.endsWith("ServiceImpl.$fileExt")

        when {
            isController() -> {
                return listOf(
                    ChatContextItem(
                        JavaTechStackContextProvider::class,
                        "You are working on a project that uses ${techStacks.coreFrameworks.keys.joinToString(",")} to build RESTful APIs."
                    )
                )
            }

            isService() -> {
                return listOf(
                    ChatContextItem(
                        JavaTechStackContextProvider::class,
                        "You are working on a project that uses ${techStacks.coreFrameworks.keys.joinToString(",")} to build business logic."
                    )
                )
            }
        }

        return listOf(
            ChatContextItem(
                JavaTechStackContextProvider::class,
                "You are working on a project that uses ${techStacks.coreFrameworks.keys.joinToString(",")} to build business logic."
            )
        )
    }

    private fun prepareLibrary(): TestStack {
        val project = ProjectManager.getInstance().openProjects.firstOrNull() ?: return TestStack()
        val basePath = project.basePath ?: return TestStack()
        val projectData = ProjectDataManager.getInstance().getExternalProjectData(
            project, GradleConstants.SYSTEM_ID, basePath
        )

        val testStack = TestStack()

        projectData?.externalProjectStructure?.children?.filter {
            it.data is LibraryData
        }?.map {
            it.data as LibraryData
        }?.forEach {
            val name = it.groupId + ":" + it.artifactId
            when {
                name.contains("spring-boot-starter-web") -> {
                    testStack.coreFrameworks.putIfAbsent("Spring Boot Starter", true)
                }
                //  org.springframework.boot:spring-boot-starter-jdbc
                name.contains("org.springframework.boot:spring-boot-starter-jdbc") -> {
                    testStack.coreFrameworks.putIfAbsent("JDBC", true)
                }

                name.contains("org.springframework.boot:spring-boot-test") -> {
                    testStack.testFrameworks.putIfAbsent("Spring Boot Test", true)
                }

                name.contains("org.assertj:assertj-core") -> {
                    testStack.coreFrameworks.putIfAbsent("AssertJ", true)
                    testStack.testFrameworks.putIfAbsent("AssertJ", true)
                }

                name.contains("org.junit.jupiter:junit-jupiter") -> {
                    testStack.coreFrameworks.putIfAbsent("JUnit 5", true)
                    testStack.testFrameworks.putIfAbsent("JUnit 5", true)
                }

                name.contains("org.mockito:mockito-core") -> {
                    testStack.coreFrameworks.putIfAbsent("Mockito", true)
                    testStack.testFrameworks.putIfAbsent("Mockito", true)
                }

                name.contains("com.h2database:h2") -> {
                    testStack.coreFrameworks.putIfAbsent("H2", true)
                }
            }
        }

        return testStack
    }
}
