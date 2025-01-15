package cc.unitmesh.git.actions.vcs

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.actions.chat.base.ChatBaseAction
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.settings.LanguageChangedCallback.presentationText
import cc.unitmesh.devti.template.GENIUS_PRACTISES
import cc.unitmesh.devti.vcs.VcsPrompting
import cc.unitmesh.devti.statusbar.AutoDevStatus
import cc.unitmesh.devti.template.TemplateRender
import cc.unitmesh.devti.template.context.TemplateContext
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import cc.unitmesh.devti.util.parser.CodeFence
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
import com.intellij.openapi.vcs.changes.CurrentContentRevision
import com.intellij.openapi.vcs.ui.CommitMessage
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.vcs.commit.CommitWorkflowUi
import com.intellij.vcs.log.VcsLogFilterCollection
import com.intellij.vcs.log.VcsLogProvider
import com.intellij.vcs.log.impl.VcsProjectLog
import com.intellij.vcs.log.visible.filters.VcsLogFilterObject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class CommitMessageSuggestionAction : ChatBaseAction() {

    init {
        presentationText("settings.autodev.others.commitMessage", templatePresentation)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    private val logger = logger<CommitMessageSuggestionAction>()

    private var currentJob: Job? = null

    override fun getActionType(): ChatActionType = ChatActionType.GEN_COMMIT_MESSAGE

    override fun update(e: AnActionEvent) {
        val data = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL)
        if (data == null) {
            e.presentation.icon = AutoDevStatus.WAITING.icon
            e.presentation.isEnabled = false
            return
        }

        val prompting = e.project?.service<VcsPrompting>()
        val changes: List<Change> = prompting?.getChanges() ?: listOf()

        e.presentation.icon = AutoDevStatus.Ready.icon
        e.presentation.isEnabled = changes.isNotEmpty()
    }

    override fun executeAction(event: AnActionEvent) {
        val project = event.project ?: return
        val commitWorkflowUi = VcsUtil.getCommitWorkFlowUi(event) ?: return
        val changes = getChanges(commitWorkflowUi) ?: return
        val diffContext = project.service<VcsPrompting>().prepareContext(changes)

        if (diffContext.isEmpty() || diffContext == "\n") {
            logger.warn("Diff context is empty or cannot get enough useful context.")
            AutoDevNotifications.notify(project, "Diff context is empty or cannot get enough useful context.")
            return
        }

        val editorField = (event.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL) as CommitMessage).editorField
        // empty commit message before generating
        val originText = editorField.editor?.selectionModel?.selectedText ?: ""
        currentJob?.cancel()
        editorField.text = ""

        ApplicationManager.getApplication().executeOnPooledThread {
            val prompt = generateCommitMessage(diffContext, project, originText)

            logger.info("Start generating commit message.")
            logger.info(prompt)

            event.presentation.icon = AutoDevStatus.InProgress.icon
            try {
                val stream = LlmFactory().create(project).stream(prompt, "", false)
                currentJob = AutoDevCoroutineScope.scope(project).launch {
                    runBlocking {
                        stream
                            .onCompletion {
                                event.presentation.icon = AutoDevStatus.Ready.icon
                            }
                            .cancellable().collect {
                                invokeLater {
                                    if (this@launch.isActive) editorField.text += it
                                }
                            }
                    }

                    val text = editorField.text
                    if (isActive && text.startsWith("```") && text.endsWith("```")) {
                        invokeLater {
                            editorField.text = CodeFence.parse(text).text
                        }
                    } else {
                        invokeLater {
                            editorField.text = text.removePrefix("```\n").removeSuffix("```")
                        }
                    }
                }
            } catch (e: Exception) {
                event.presentation.icon = AutoDevStatus.Error.icon
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

    private fun generateCommitMessage(diff: String, project: Project, originText: String): String {
        val templateRender = TemplateRender(GENIUS_PRACTISES)
        val template = templateRender.getTemplate("gen-commit-msg.vm")

        val historyExamples = try {
            findExampleCommitMessages(project) ?: ""
        } catch (e: Exception) {
            logger.warn("Cannot get example commit messages.", e)
            ""
        }

        templateRender.context = CommitMsgGenContext(
            historyExamples = historyExamples,
            diffContent = diff,
            originText = originText
        )
        val prompter = templateRender.renderTemplate(template)

        logger.info("Prompt: $prompter")
        return prompter
    }

    fun getChanges(commitWorkflowUi: CommitWorkflowUi): List<Change>? {
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
}


data class CommitMsgGenContext(
    var historyExamples: String = "",
    var diffContent: String = "",
    // the origin commit message which is to be optimized
    val originText: String = "",
) : TemplateContext
