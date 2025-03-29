package cc.unitmesh.devti.gui.planner

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change

class PlannerResultSummary(
    val project: Project,
    val changes: List<Change>
) {
    fun getDiffContent(change: Change) {
        change.afterRevision?.file?.name
        change.afterRevision?.file?.path
        change.afterRevision?.content
    }
}