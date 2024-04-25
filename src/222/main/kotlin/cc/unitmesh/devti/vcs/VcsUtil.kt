package cc.unitmesh.devti.vcs

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.CurrentContentRevision
import com.intellij.vcs.commit.AbstractCommitWorkflowHandler
import com.intellij.vcs.commit.CommitWorkflowUi

object VcsUtil {
    fun getCommitWorkFlowUi(e: AnActionEvent): CommitWorkflowUi? {
        val commitWorkFlowHandler = e.getData(VcsDataKeys.COMMIT_WORKFLOW_HANDLER)
        return (commitWorkFlowHandler as? AbstractCommitWorkflowHandler<*, *>)?.ui ?: return null
    }
}