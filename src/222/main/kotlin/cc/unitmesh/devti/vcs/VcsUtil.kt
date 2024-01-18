package cc.unitmesh.devti.vcs

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change

object VcsUtil {
    fun getChanges(e: AnActionEvent): List<Change>? {
        val commitWorkflowUi = e.getData(VcsDataKeys.CHANGES) ?: return null
        return commitWorkflowUi.toList()
    }
}