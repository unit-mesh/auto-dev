package cc.unitmesh.devti.language.git

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsBundle
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.changes.LocalChangeList
import com.intellij.vcs.commit.ChangeListCommitState
import com.intellij.vcs.commit.LocalChangesCommitter
import com.intellij.vcs.commit.ShowNotificationCommitResultHandler

object GitUtil {
    fun doCommit(myProject: Project, list: LocalChangeList, commitMsg: String) {
        val commitState = ChangeListCommitState(list, list.changes.toList(), commitMsg)
        val committer = LocalChangesCommitter(myProject, commitState, CommitContext())
        committer.addResultHandler(ShowNotificationCommitResultHandler(committer))
        committer.runCommit(VcsBundle.message("commit.changes"), true)
    }
}