package cc.unitmesh.idea.provider

import cc.unitmesh.devti.provider.TechStackProvider
import cc.unitmesh.devti.prompting.code.TestStack
import com.intellij.openapi.externalSystem.model.project.LibraryData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.project.ProjectManager
import org.jetbrains.plugins.gradle.util.GradleConstants

class JvmTechStackService: TechStackProvider() {
    override fun prepareLibrary(): TestStack {
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
