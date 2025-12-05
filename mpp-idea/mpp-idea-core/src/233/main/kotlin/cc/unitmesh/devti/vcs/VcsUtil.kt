package cc.unitmesh.devti.vcs

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.vcs.commit.CommitWorkflowUi

object VcsUtil {
    fun getCommitWorkFlowUi(e: AnActionEvent): CommitWorkflowUi? {
        return e.getData(VcsDataKeys.COMMIT_WORKFLOW_UI)
    }
}