package cc.unitmesh.idea.provider

import cc.unitmesh.devti.provider.TechStackProvider
import cc.unitmesh.devti.prompting.code.TestStack
import com.intellij.openapi.externalSystem.model.project.LibraryData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.project.ProjectManager
import org.jetbrains.plugins.gradle.util.GradleConstants

class JvmTechStackService: TechStackProvider() {
    override fun prepareLibrary(): TestStack {
        val project = ProjectManager.getInstance().defaultProject
        val projectData = ProjectDataManager.getInstance().getExternalProjectData(
            project, GradleConstants.SYSTEM_ID, project.basePath!!
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
                    testStack.controller.putIfAbsent("Spring Boot Starter", true)
                }
                //  org.springframework.boot:spring-boot-starter-jdbc
                name.contains("org.springframework.boot:spring-boot-starter-jdbc") -> {
                    testStack.controller.putIfAbsent("JDBC", true)
                }

                name.contains("org.springframework.boot:spring-boot-test") -> {
                    testStack.service.putIfAbsent("Spring Boot Test", true)
                }

                name.contains("org.assertj:assertj-core") -> {
                    testStack.controller.putIfAbsent("AssertJ", true)
                    testStack.service.putIfAbsent("AssertJ", true)
                }

                name.contains("org.junit.jupiter:junit-jupiter") -> {
                    testStack.controller.putIfAbsent("JUnit 5", true)
                    testStack.service.putIfAbsent("JUnit 5", true)
                }

                name.contains("org.mockito:mockito-core") -> {
                    testStack.controller.putIfAbsent("Mockito", true)
                    testStack.service.putIfAbsent("Mockito", true)
                }

                name.contains("com.h2database:h2") -> {
                    testStack.controller.putIfAbsent("H2", true)
                }
            }
        }

        return testStack
    }
}
