package cc.unitmesh.devti.language.git

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.changes.LocalChangeList
import com.intellij.vcs.commit.ChangeListCommitState
import com.intellij.vcs.commit.LocalChangesCommitter

object GitUtil {
    fun doCommit(myProject: Project, list: LocalChangeList, commitMsg: String) {
        val commitState = ChangeListCommitState(list, list.changes.toList(), commitMsg)
        val committer = LocalChangesCommitter(myProject, commitState, CommitContext())
        committer.runCommit("Commit", false)
    }
}