package cc.unitmesh.devti.actions.vcs

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vcs.VcsDataKeys

class PrepushReviewAction : CodeReviewAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        val selectList = event.getData(VcsDataKeys.CHANGES) ?: return

        doReviewWithChanges(project, listOf(), selectList, listOf())
    }
}
