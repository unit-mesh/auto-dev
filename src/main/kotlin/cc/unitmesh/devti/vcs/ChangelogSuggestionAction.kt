package cc.unitmesh.devti.vcs

import cc.unitmesh.devti.context.ClassContextProvider
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.vcs.log.VcsLogDataKeys

// TODO: for 232 , we
class ChangelogSuggestionAction : AnAction() {
    companion object {
        val logger = Logger.getInstance(ChangelogSuggestionAction::class.java)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project

        val vcsLog = e.getData(VcsLogDataKeys.VCS_LOG)
        val stringList = vcsLog?.let { log ->
            log.selectedShortDetails.map { it.fullMessage }
        }

        println(stringList)
    }
}
