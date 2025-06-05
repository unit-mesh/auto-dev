package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.command.InsCommand
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.git.GitUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.LocalChangeList

class CommitInsCommand(val myProject: Project, val commitMsg: String) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.COMMIT

    /**
     * [com.intellij.openapi.vcs.changes.shelf.ShelveChangesAction] to trigger the action
     *
     * [com.intellij.openapi.vcs.changes.shelf.ShelveChangesCommitExecutor]
     *
     * [com.intellij.openapi.vcs.changes.shelf.ShelvedChangesViewManager] to manage the shelve changes
     */
    override suspend fun execute(): String {
        val changeListManager = ChangeListManager.getInstance(myProject)
        val changeList: LocalChangeList = changeListManager.defaultChangeList
        GitUtil.doCommit(myProject, changeList, commitMsg)
        return "Commited for $changeList"
    }
}
