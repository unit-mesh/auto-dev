package cc.unitmesh.devti.actions.vcs

import cc.unitmesh.devti.vcs.VcsPrompting
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change

class PrepushReviewAction : CodeReviewAction() {
    override fun update(e: AnActionEvent) {
        val data = e.getData(VcsDataKeys.CHANGES)
        if (data == null) {
            e.presentation.isEnabled = false
            return
        }

        val prompting = e.project?.service<VcsPrompting>()
        val changes: List<Change> = prompting?.hasChanges() ?: listOf()

        e.presentation.isEnabled = changes.isNotEmpty()
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        val selectList = event.getData(VcsDataKeys.CHANGES) ?: return

        doReviewWithChanges(project, listOf(), selectList, listOf())
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
