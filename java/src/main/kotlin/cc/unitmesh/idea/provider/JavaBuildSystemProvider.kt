package cc.unitmesh.idea.provider

import cc.unitmesh.devti.provider.BuildSystemProvider
import cc.unitmesh.devti.template.context.DockerfileContext
import cc.unitmesh.idea.detectLanguageLevel
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.service.ui.completion.TextCompletionInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import org.jetbrains.plugins.gradle.service.project.GradleTasksIndices
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

        val javaVersion = detectLanguageLevel(project, null)

        return DockerfileContext(
            buildToolName = buildToolName,
            buildToolVersion = "",
            languageName = "Java",
            languageVersion = "$javaVersion",
            taskString = taskString
        )
    }

    companion object {

        val GRADLE_COMPLETION_COMPARATOR = Comparator<String> { o1, o2 ->
            when {
                o1.startsWith("--") && o2.startsWith("--") -> o1.compareTo(o2)
                o1.startsWith("-") && o2.startsWith("--") -> -1
                o1.startsWith("--") && o2.startsWith("-") -> 1
                o1.startsWith(":") && o2.startsWith(":") -> o1.compareTo(o2)
                o1.startsWith(":") && o2.startsWith("-") -> -1
                o1.startsWith("-") && o2.startsWith(":") -> 1
                o2.startsWith("-") -> -1
                o2.startsWith(":") -> -1
                o1.startsWith("-") -> 1
                o1.startsWith(":") -> 1
                else -> o1.compareTo(o2)
            }
        }
    }
}

