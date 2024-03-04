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

    private val logger = logger<CommitMessageSuggestionAction>()

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

        val commitMessageUi = event.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL) as CommitMessage
        // empty commit message before generating
        commitMessageUi.editorField.text = ""

        ApplicationManager.getApplication().executeOnPooledThread() {
            val prompt = generateCommitMessage(diffContext, project)

            logger.info("Start generating commit message.")
            logger.info(prompt)

            event.presentation.icon = AutoDevStatus.InProgress.icon
            val stream = LlmFactory().create(project).stream(prompt, "", false)

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

    /**
     * Finds example commit messages based on the project's VCS log, takes the first three commits.
     * If the no user or user has not committed anything yet, the current branch name is used instead.
     *
     * @param project The project for which to find example commit messages.
     * @return A string containing example commit messages, or null if no example messages are found.
     */
    private fun findExampleCommitMessages(project: Project): String? {
        val logProviders = VcsProjectLog.getLogProviders(project)
        val entry = logProviders.entries.firstOrNull() ?: return null

        val logProvider = entry.value
        val branch = logProvider.getCurrentBranch(entry.key) ?: return null
        val user = logProvider.getCurrentUser(entry.key)

        val logFilter = if (user != null) {
            VcsLogFilterObject.collection(VcsLogFilterObject.fromUser(user, setOf()))
        } else {
            VcsLogFilterObject.collection(VcsLogFilterObject.fromBranch(branch))
        }

        return collectExamples(logProvider, entry.key, logFilter)
    }

    /**
     * Collects examples from the VcsLogProvider based on the provided filter.
     *
     * @param logProvider The VcsLogProvider used to retrieve commit information.
     * @param root The root VirtualFile of the project.
     * @param filter The VcsLogFilterCollection used to filter the commits.
     * @return A string containing the collected examples, or null if no examples are found.
     */
    private fun collectExamples(
        logProvider: VcsLogProvider,
        root: VirtualFile,
        filter: VcsLogFilterCollection
    ): String? {
        val commits = logProvider.getCommitsMatchingFilter(root, filter, 3)

        if (commits.isEmpty()) return null

        val builder = StringBuilder("")
        val commitIds = commits.map { it.id.asString() }

        logProvider.readMetadata(root, commitIds) {
            val shortMsg = it.fullMessage.split("\n").firstOrNull() ?: it.fullMessage
            builder.append(shortMsg).append("\n")
        }

        return builder.toString()
    }

    private fun generateCommitMessage(diff: String, project: Project): String {
        val templateRender = TemplateRender("genius/practises")
        val template = templateRender.getTemplate("gen-commit-msg.vm")

        val historyExample = try {
            findExampleCommitMessages(project) ?: ""
        } catch (e: Exception) {
            logger.warn("Cannot get example commit messages.", e)
            ""
        }

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
