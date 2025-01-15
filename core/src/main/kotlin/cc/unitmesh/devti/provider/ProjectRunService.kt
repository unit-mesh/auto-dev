package cc.unitmesh.devti.provider

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

interface ProjectRunService {
    fun isAvailable(project: Project): Boolean

    fun run(project: Project, taskName: String)

    /**
     * Return List of available tasks
     *
     * This function takes in a Project, CompletionParameters, and CompletionResultSet as parameters
     * and returns a Map of LookupElement to Priority. It is used to lookup available tasks.
     *
     * @param project the project in which the tasks are available
     * @param parameters the completion parameters for the task lookup
     * @param result the completion result set to store the lookup results
     * @return a Map of LookupElement to Priority representing the available tasks
     */
    fun lookupAvailableTask(
        project: Project,
        parameters: CompletionParameters,
        result: CompletionResultSet,
    ): List<LookupElement> {
        return listOf()
    }

    fun tasks(project: Project): List<String> {
        return listOf()
    }

    companion object {
        val EP_NAME: ExtensionPointName<ProjectRunService> = ExtensionPointName("cc.unitmesh.runProjectService")

        fun all(): List<ProjectRunService> {
            return EP_NAME.extensionList
        }

        fun provider(project: Project): ProjectRunService? {
            val projectRunServices = EP_NAME.extensionList
            return projectRunServices.firstOrNull { it.isAvailable(project) }
        }
    }
}
