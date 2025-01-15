package cc.unitmesh.devti.provider

import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.openapi.externalSystem.service.ui.completion.TextCompletionInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * The `BuildTool` interface provides a set of methods for managing build tasks in a project.
 * It is designed to be implemented by classes that provide specific build tool functionality.
 *
 * This interface includes methods for preparing library data, collecting tasks, and configuring run tasks.
 *
 * Implementations of this interface are expected to provide specific functionality for different build tools.
 */
interface BuildTool {
    fun toolName(): String

    fun prepareLibraryData(project: Project): List<CommonLibraryData>?

    fun collectTasks(project: Project): List<TextCompletionInfo>

    fun configureRun(project: Project, taskName: String, virtualFile: VirtualFile?): LocatableConfigurationBase<*>?
}

data class CommonLibraryData(val groupId: String?, val artifactId: String?, val version: String?) {
    fun prettyString(): String {
        return "$groupId:$artifactId:$version"
    }
}