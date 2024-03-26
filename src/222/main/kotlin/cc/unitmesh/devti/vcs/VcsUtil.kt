package cc.unitmesh.devti.vcs

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.vcs.changes.Change

object VcsUtil {
    fun getChanges(e: AnActionEvent): List<Change>? {
        val prompting = e.project?.service<VcsPrompting>() ?: return null
        val changes = prompting.getChanges()
        return changes.ifEmpty { null }
    }
}