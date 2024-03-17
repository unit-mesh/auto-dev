package cc.unitmesh.devti.language.compiler.exec

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.changes.LocalChangeList
import com.intellij.vcs.commit.ChangeListCommitState
import com.intellij.vcs.commit.LocalChangesCommitter

class CommitInsCommand(val myProject: Project, val commitMsg: String) : InsCommand {
    override fun execute(): String {
        val changeListManager = ChangeListManager.getInstance(myProject)
        changeListManager.changeLists.forEach {
            val list: LocalChangeList = changeListManager.getChangeList(it.id) ?: return@forEach
            val commitState = ChangeListCommitState(it, list.changes.toList(), commitMsg)
            val committer = LocalChangesCommitter(myProject, commitState, CommitContext())
            committer.runCommit("Commit", false)
        }

        return "Committing..."
    }
}