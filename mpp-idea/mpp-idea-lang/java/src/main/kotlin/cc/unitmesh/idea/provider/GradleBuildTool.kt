package cc.unitmesh.idea.provider

import cc.unitmesh.devti.provider.BuildTool
import cc.unitmesh.devti.provider.CommonLibraryData
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.openapi.externalSystem.model.project.LibraryData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.service.ui.completion.TextCompletionInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import org.jetbrains.plugins.gradle.service.project.GradleTasksIndices
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.gradle.util.GradleTaskData

class GradleBuildTool: BuildTool {
    override fun toolName(): String = "Gradle"

    override fun prepareLibraryData(project: Project): List<CommonLibraryData>? {
        val basePath = project.basePath ?: return null
        val projectData = ProjectDataManager.getInstance().getExternalProjectData(
            project, GradleConstants.SYSTEM_ID, basePath
        )

        val libraryDataList: List<LibraryData>? = projectData?.externalProjectStructure?.children?.filter {
            it.data is LibraryData
        }?.map {
            it.data as LibraryData
        }

        return libraryDataList?.map {
            CommonLibraryData(it.groupId, it.artifactId, it.version)
        }
    }

    override fun collectTasks(project: Project): List<TextCompletionInfo> {
        val indices = GradleTasksIndices.getInstance(project)
        val tasks = indices.findTasks(project.guessProjectDir()!!.path)
            .filterNot { it.isInherited }
            .groupBy { it.name }
            .map { TextCompletionInfo(it.key, it.value.first().description) }
            .sortedWith(
                Comparator.comparing<TextCompletionInfo?, @NlsSafe String?>(
                    { it.text },
                    JAVA_TASK_COMPLETION_COMPARATOR
                )
            )
        return tasks
    }

    override fun configureRun(
        project: Project,
        taskName: String,
        virtualFile: VirtualFile?,
    ): LocatableConfigurationBase<*> {
        val runManager = RunManager.getInstance(project)
        val configuration = runManager.createConfiguration(
            taskName,
            GradleExternalTaskConfigurationType::class.java
        )
        val runConfiguration = configuration.configuration as GradleRunConfiguration
        runConfiguration.isDebugServerProcess = false
        runConfiguration.settings.externalProjectPath = project.guessProjectDir()?.path
        runConfiguration.rawCommandLine = taskName
        runManager.addConfiguration(configuration)
        runManager.selectedConfiguration = configuration
        return runConfiguration
    }

    companion object {
        fun collectGradleTasksData(project: Project): List<GradleTaskData> {
            val indices = GradleTasksIndices.getInstance(project)
            val tasks = indices.findTasks(project.guessProjectDir()!!.path)
            return tasks
        }
    }
}

val JAVA_TASK_COMPLETION_COMPARATOR = Comparator<String> { o1, o2 ->
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