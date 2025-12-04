package cc.unitmesh.devti.language.compiler.exec.vcs

import cc.unitmesh.devti.command.InsCommand
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.git.GitUtil
import cc.unitmesh.devti.sketch.AutoSketchMode
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.LocalChangeList

class CommitInsCommand(val myProject: Project, val commitMsg: String) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.COMMIT

    /**
     * [com.intellij.openapi.vcs.changes.shelf.ShelveChangesAction] to trigger the action
     *
     * [com.intellij.openapi.vcs.changes.shelf.ShelveChangesCommitExecutor]
     *
     * [com.intellij.openapi.vcs.changes.shelf.ShelvedChangesViewManager] to manage the shelf changes
     */
    override suspend fun execute(): String {
        if (AutoSketchMode.getInstance(myProject).isEnable) {
            invokeShelveChangesAction()
            return "Commit by UI will depends by user selection"
        } else {
            val changeListManager = ChangeListManager.getInstance(myProject)
            val changeList: LocalChangeList = changeListManager.defaultChangeList
            GitUtil.doCommit(myProject, changeList, commitMsg)
            return "Commited for $changeList"
        }
    }

    private fun invokeShelveChangesAction() {
        val actionManager = ActionManager.getInstance()
        val shelveAction = actionManager.getAction("ChangesView.Shelve")

        if (shelveAction != null) {
            ApplicationManager.getApplication().invokeLater({
                val dataContext = DataManager.getInstance().getDataContext()
                val event = AnActionEvent.createFromAnAction(
                    shelveAction,
                    null,
                    "",
                    dataContext
                )
                // Use ActionUtil to properly invoke the action instead of calling actionPerformed directly
                ActionUtil.performActionDumbAwareWithCallbacks(shelveAction, event)
            }, ModalityState.NON_MODAL)
        }
    }
}
