package cc.unitmesh.idea.provider

import cc.unitmesh.devti.provider.BuildSystemProvider
import cc.unitmesh.devti.template.DockerfileContext
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.service.ui.completion.TextCompletionInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.util.lang.JavaVersion
import org.jetbrains.plugins.gradle.service.project.GradleTasksIndices
import org.jetbrains.plugins.gradle.service.project.GradleTasksIndicesImpl
import org.jetbrains.plugins.gradle.service.project.GradleTasksIndicesImpl.Companion.GRADLE_COMPLETION_COMPARATOR
import org.jetbrains.plugins.gradle.util.GradleConstants


open class JavaBuildSystemProvider : BuildSystemProvider() {
    override fun collect(project: Project): DockerfileContext {
        val projectDataManager = ProjectDataManager.getInstance()
        val buildToolName: String
        var taskString = ""

        val gradleInfo = projectDataManager.getExternalProjectsData(project, GradleConstants.SYSTEM_ID)
        if (gradleInfo.isNotEmpty()) {
            buildToolName = "Gradle"
            val indices = GradleTasksIndices.getInstance(project)

            val tasks = indices.findTasks(project.guessProjectDir()!!.path)
                .filterNot { it.isInherited }
                .groupBy { it.name }
                .map { TextCompletionInfo(it.key, it.value.first().description) }
                .sortedWith(Comparator.comparing({ it.text }, GRADLE_COMPLETION_COMPARATOR))

            taskString = tasks.joinToString(" ") { it.text }
        } else {
            buildToolName = "Maven"
        }

        val javaVersion = JavaVersion.current()
        val javaVersionStr = "${javaVersion.feature}"

        return DockerfileContext(
            buildToolVersion = "",
            buildToolName = buildToolName,
            languageName = "Java",
            languageVersion = javaVersionStr,
            taskString = taskString
        )
    }
}
