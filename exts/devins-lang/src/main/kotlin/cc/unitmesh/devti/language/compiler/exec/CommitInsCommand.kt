package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.devin.InsCommand
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.git.GitUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.LocalChangeList

class CommitInsCommand(val myProject: Project, val commitMsg: String) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.COMMIT

    override suspend fun execute(): String {
        val changeListManager = ChangeListManager.getInstance(myProject)
        val changeList: LocalChangeList = changeListManager.defaultChangeList
        GitUtil.doCommit(myProject, changeList, commitMsg)

        return "Commited for $changeList"
    }
}
