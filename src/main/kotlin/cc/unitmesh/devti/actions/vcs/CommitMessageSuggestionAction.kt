package cc.unitmesh.devti.actions.vcs

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.actions.chat.base.ChatBaseAction
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.vcs.VcsPrompting
import cc.unitmesh.devti.statusbar.AutoDevStatus
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.CurrentContentRevision
import com.intellij.openapi.vcs.ui.CommitMessage
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.vcs.commit.CommitWorkflowUi
import com.intellij.vcs.log.TimedVcsCommit
import com.intellij.vcs.log.VcsCommitMetadata
import com.intellij.vcs.log.VcsLogFilterCollection
import com.intellij.vcs.log.VcsLogProvider
import com.intellij.vcs.log.impl.VcsProjectLog
import com.intellij.vcs.log.visible.filters.VcsLogFilterObject
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.*

class CommitMessageSuggestionAction : ChatBaseAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    val logger = logger<CommitMessageSuggestionAction>()

    override fun getActionType(): ChatActionType = ChatActionType.GEN_COMMIT_MESSAGE

    override fun update(e: AnActionEvent) {
        val data = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL)
        if (data == null) {
            e.presentation.icon = AutoDevStatus.WAITING.icon
            e.presentation.isEnabled = false
            return
        }

        val prompting = e.project?.service<VcsPrompting>()
        val changes: List<Change> = prompting?.hasChanges() ?: listOf()

        e.presentation.icon = AutoDevStatus.Ready.icon
        e.presentation.isEnabled = changes.isNotEmpty()
    }

    private fun getChanges(e: AnActionEvent): List<Change>? {
        val commitWorkflowUi = e.getData(VcsDataKeys.COMMIT_WORKFLOW_UI) ?: return null

        val changes = commitWorkflowUi.getIncludedChanges()
        val unversionedFiles = commitWorkflowUi.getIncludedUnversionedFiles()

        val unversionedFileChanges = unversionedFiles.map {
            Change(null, CurrentContentRevision(it))
        }

        if (changes.isNotEmpty() || unversionedFileChanges.isNotEmpty()) {
            return changes + unversionedFileChanges
        }

        return null
    }

    override fun executeAction(event: AnActionEvent) {
        val project = event.project ?: return
        val changes = getChanges(event) ?: return
        val diffContext = project.service<VcsPrompting>().prepareContext(changes)

        if (diffContext.isEmpty() || diffContext == "\n") {
            logger.warn("Diff context is empty or cannot get enough useful context.")
            AutoDevNotifications.notify(project, "Diff context is empty or cannot get enough useful context.")
            return
        }

        val prompt = generateCommitMessage(diffContext, project)
        val commitMessageUi = event.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL) as CommitMessage

        // empty commit message before generating
        commitMessageUi.editorField.text = ""

        logger.info("Start generating commit message.")
        logger.info(prompt)

        event.presentation.icon = AutoDevStatus.InProgress.icon
        val stream = LlmFactory().create(project).stream(prompt, "", false)

        ApplicationManager.getApplication().executeOnPooledThread() {
            runBlocking {
                stream.cancellable().collect {
                    invokeLater {
                        commitMessageUi.editorField.text += it
                    }
                }

                event.presentation.icon = AutoDevStatus.Ready.icon
            }
        }
    }

    private fun findExampleCommitMessages(project: Project): String? {
        val logProviders = VcsProjectLog.getLogProviders(project)
        val entry = logProviders.entries.firstOrNull() ?: return null

        val logProvider = entry.value
        val branch = logProvider.getCurrentBranch(entry.key) ?: return null
        val user = logProvider.getCurrentUser(entry.key)
        val fromBranch = VcsLogFilterObject.fromBranch(branch)

        val filter = if (user != null) {
            val fromUser = VcsLogFilterObject.fromUser(user, setOf())
            VcsLogFilterObject.collection(fromUser)
        } else {
            VcsLogFilterObject.collection(fromBranch)
        }

        return collectExamples(logProvider, entry.key, filter)
    }

    private fun collectExamples(
        logProvider: VcsLogProvider,
        root: VirtualFile,
        filter: VcsLogFilterCollection
    ): String? {
        val commits = logProvider.getCommitsMatchingFilter(root, filter, 3)

        if (commits.isEmpty()) return null

        val builder = StringBuilder("")
        val commitIds = commits.map { (it as TimedVcsCommit).id.asString() }

        val metadataCallback: (VcsCommitMetadata) -> Unit = {
            builder.append(it.fullMessage).append("\n")
        }

        logProvider.readMetadata(root, commitIds, metadataCallback)

        return builder.toString()
    }

    private fun generateCommitMessage(diff: String, project: Project): String {
        return """Write a cohesive yet descriptive commit message for a given diff. 
Make sure to include both information What was changed and Why.
Start with a short sentence in imperative form, no more than 50 characters long.
Then leave an empty line and continue with a more detailed explanation, if necessary.
Explanation should have less than 200 characters.

Examples:
- fix(authentication): add password regex pattern
- feat(storage): add new test cases
- test(java): fix test case for user controller
History Examples:
${findExampleCommitMessages(project) ?: "No example found."}

Diff:

```diff
$diff
```

"""
    }


}
