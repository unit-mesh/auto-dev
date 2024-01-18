package cc.unitmesh.devti.actions.vcs

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.actions.chat.base.ChatBaseAction
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.vcs.VcsPrompting
import cc.unitmesh.devti.statusbar.AutoDevStatus
import cc.unitmesh.devti.template.TemplateRender
import cc.unitmesh.devti.vcs.VcsUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.ui.CommitMessage
import com.intellij.openapi.vfs.VirtualFile
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

    override fun executeAction(event: AnActionEvent) {
        val project = event.project ?: return
        val changes = VcsUtil.getChanges(event) ?: return
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
        val templateRender = TemplateRender("genius/practises")
        val template = templateRender.getTemplate("gen-commit-msg.vm")

        val historyExample = findExampleCommitMessages(project) ?: ""
        templateRender.context = CommitMsgGenContext(
            historyExample = historyExample,
            diffContent = diff,
        )
        val prompter = templateRender.renderTemplate(template)

        logger.info("Prompt: $prompter")
        return prompter
    }
}


data class CommitMsgGenContext(
    var historyExample: String = "",
    var diffContent: String = "",
)
