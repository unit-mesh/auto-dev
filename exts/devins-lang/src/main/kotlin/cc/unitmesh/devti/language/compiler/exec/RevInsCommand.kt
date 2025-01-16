package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.devin.InsCommand
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import cc.unitmesh.devti.provider.RevisionProvider
import com.intellij.openapi.project.Project


/**
 * RevAutoCommand is used to execute a command that retrieves the committed change list for a given revision using Git.
 *
 * @param myProject the Project instance associated with the command
 * @param revision the Git revision for which the committed change list is to be retrieved
 *
 */
class RevInsCommand(private val myProject: Project, private val revision: String) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.REV

    override suspend fun execute(): String? {
        return RevisionProvider.provide()?.let {
            val changes = it.fetchChanges(myProject, revision)
            return changes ?: "No changes found for revision $revision"
        } ?: "No revision provider found"
    }
}
