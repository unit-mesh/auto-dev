package cc.unitmesh.devti.vcs

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.CurrentContentRevision

object VcsUtil {
    fun getCommitWorkFlowUi(e: AnActionEvent): CommitWorkflowUi? {
        return e.getData(VcsDataKeys.COMMIT_WORKFLOW_UI) ?: return null
    }
}