package cc.unitmesh.git.actions.vcs

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.actions.chat.base.ChatBaseAction
import cc.unitmesh.devti.flow.kanban.impl.GitHubIssue
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.settings.locale.LanguageChangedCallback.presentationText
import cc.unitmesh.devti.template.GENIUS_PRACTISES
import cc.unitmesh.devti.vcs.VcsPrompting
import cc.unitmesh.devti.statusbar.AutoDevStatus
import cc.unitmesh.devti.template.TemplateRender
import cc.unitmesh.devti.template.context.TemplateContext
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import cc.unitmesh.devti.util.parser.CodeFence
import cc.unitmesh.devti.vcs.VcsUtil
import com.intellij.ide.TextCopyProvider
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys.COPY_PROVIDER
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.CurrentContentRevision
import com.intellij.openapi.vcs.ui.CommitMessage
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.speedSearch.SpeedSearchUtil.applySpeedSearchHighlighting
import com.intellij.util.containers.nullize
import com.intellij.util.ui.JBUI.scale
import com.intellij.vcs.commit.CommitWorkflowUi
import com.intellij.vcs.log.VcsLogFilterCollection
import com.intellij.vcs.log.VcsLogProvider
import com.intellij.vcs.log.impl.VcsProjectLog
import com.intellij.vcs.log.visible.filters.VcsLogFilterObject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.kohsuke.github.GHIssue
import org.kohsuke.github.GHIssueState
import java.awt.Point
import javax.swing.JList
import javax.swing.ListSelectionModel.SINGLE_SELECTION

data class IssueDisplayItem(val issue: GHIssue, val displayText: String)

class CommitMessageSuggestionAction : ChatBaseAction() {
    private val logger = logger<CommitMessageSuggestionAction>()

    init {
        presentationText("settings.autodev.others.commitMessage", templatePresentation)
        isEnabledInModalContext = true
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    private var currentJob: Job? = null
    private var selectedIssue: IssueDisplayItem? = null
    private var currentChanges: List<Change>? = null
    private var currentEvent: AnActionEvent? = null
    private var isGitHubRepository: Boolean = false

    override fun getActionType(): ChatActionType = ChatActionType.GEN_COMMIT_MESSAGE

    override fun update(e: AnActionEvent) {
        val project = e.project
        val data = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL)

        if (data == null || project == null) {
            e.presentation.icon = AutoDevStatus.WAITING.icon
            e.presentation.isEnabled = false
            return
        }

        val prompting = project.service<VcsPrompting>()
        val changes: List<Change> = prompting.getChanges()

        // Check if it's a GitHub repository (safe to call in BGT)
        isGitHubRepository = GitHubIssue.isGitHubRepository(project)

        // Update presentation text based on whether it's a GitHub repository
        if (isGitHubRepository) {
            e.presentation.text = "Smart Commit Message (GitHub Enhanced)"
            e.presentation.description = "Generate commit message with AI or GitHub issue integration"
        } else {
            e.presentation.text = "Smart Commit Message"
            e.presentation.description = "Generate commit message with AI"
        }

        e.presentation.icon = AutoDevStatus.Ready.icon
        e.presentation.isEnabled = changes.isNotEmpty()
    }

    override fun executeAction(event: AnActionEvent) {
        val project = event.project ?: return
        val commitMessage = getCommitMessage(event) ?: return

        val commitWorkflowUi = VcsUtil.getCommitWorkFlowUi(event)
        if (commitWorkflowUi == null) {
            AutoDevNotifications.notify(project, "Cannot get commit workflow UI.")
            return
        }

        val changes = getChanges(commitWorkflowUi)
        if (changes == null || changes.isEmpty()) {
            AutoDevNotifications.notify(project, "No changes to commit. Do you select any files?")
            return
        }

        // Store current state for later use
        currentChanges = changes
        currentEvent = event
        selectedIssue = null

        // For GitHub repositories, show issue selection popup directly
        // For non-GitHub repositories, generate AI commit message directly
        if (isGitHubRepository) {
            generateGitHubIssueCommitMessage(project, commitMessage, event)
        } else {
            generateAICommitMessage(project, commitMessage, changes, event)
        }
    }

    private fun getCommitMessage(e: AnActionEvent) = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL) as? CommitMessage

    private fun generateGitHubIssueCommitMessage(project: Project, commitMessage: CommitMessage, event: AnActionEvent) {
        val task = object : Task.Backgroundable(project, "Loading GitHub issues", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Connecting to GitHub..."
                indicator.fraction = 0.1
                try {
                    indicator.text = "Fetching repository issues..."
                    indicator.fraction = 0.5
                    val issues = fetchGitHubIssues(project)
                    indicator.fraction = 0.9
                    ApplicationManager.getApplication().invokeLater {
                        if (issues.isEmpty()) {
                            // No issues found, fall back to AI generation
                            val changes = currentChanges ?: return@invokeLater
                            generateAICommitMessage(project, commitMessage, changes, event)
                        } else {
                            createIssuesPopup(commitMessage, issues).showInBestPositionFor(event.dataContext)
                        }
                    }
                } catch (ex: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        logger.warn("Failed to fetch GitHub issues, falling back to AI generation", ex)
                        // Fall back to AI generation when GitHub issues fetch fails
                        val changes = currentChanges ?: return@invokeLater
                        generateAICommitMessage(project, commitMessage, changes, event)
                    }
                }
            }
        }
        ProgressManager.getInstance().run(task)
    }

    private fun fetchGitHubIssues(project: Project): List<IssueDisplayItem> {
        val ghRepository =
            GitHubIssue.parseGitHubRepository(project) ?: throw IllegalStateException("Not a GitHub repository")
        return ghRepository.getIssues(GHIssueState.OPEN).map { issue ->
            val displayText = "#${issue.number} - ${issue.title}"
            IssueDisplayItem(issue, displayText)
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

    private fun createIssuesPopup(commitMessage: CommitMessage, issues: List<IssueDisplayItem>): JBPopup {
        var chosenIssue: IssueDisplayItem? = null
        var selectedIndex: Int = -1

        return JBPopupFactory.getInstance().createPopupChooserBuilder(issues)
            .setTitle("Select GitHub Issue (ESC to skip)")
            .setVisibleRowCount(10)
            .setSelectionMode(SINGLE_SELECTION)
            .setItemSelectedCallback { chosenIssue = it }
            .setItemChosenCallback {
                chosenIssue = it
            }
            .setRenderer(object : ColoredListCellRenderer<IssueDisplayItem>() {
                override fun customizeCellRenderer(
                    list: JList<out IssueDisplayItem>,
                    value: IssueDisplayItem,
                    index: Int,
                    selected: Boolean,
                    hasFocus: Boolean
                ) {
                    append("#${value.issue.number} ", com.intellij.ui.SimpleTextAttributes.GRAYED_ATTRIBUTES)
                    append(value.issue.title)
                    val labels = value.issue.labels.map { it.name }
                    if (labels.isNotEmpty()) {
                        append(" ")
                        labels.forEach { label ->
                            append("[$label] ", com.intellij.ui.SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES)
                        }
                    }
                    applySpeedSearchHighlighting(list, this, true, selected)
                }
            })
            .addListener(object : JBPopupListener {
                override fun beforeShown(event: LightweightWindowEvent) {
                    val popup = event.asPopup()
                    val relativePoint = RelativePoint(commitMessage.editorField, Point(0, -scale(3)))
                    val screenPoint = Point(relativePoint.screenPoint).apply { translate(0, -popup.size.height) }
                    popup.setLocation(screenPoint)
                }

                override fun onClosed(event: LightweightWindowEvent) {
                    // IDEA-195094 Regression: New CTRL-E in "commit changes" breaks keyboard shortcuts
                    commitMessage.editorField.requestFocusInWindow()
                    if (chosenIssue != null) {
                        // User selected an issue
                        handleIssueSelection(chosenIssue!!, commitMessage)
                    } else {
                        // User cancelled (ESC) - skip issue selection and generate with AI
                        handleSkipIssueSelection(commitMessage)
                    }
                }
            })
            .setNamerForFiltering { it.displayText }
            .setAutoPackHeightOnFiltering(true)
            .createPopup()
            .apply {
                setDataProvider { dataId ->
                    when (dataId) {
                        // default list action does not work as "CopyAction" is invoked first, but with other copy provider
                        COPY_PROVIDER.name -> object : TextCopyProvider() {
                            override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
                            override fun getTextLinesToCopy() = listOfNotNull(chosenIssue?.displayText).nullize()
                        }

                        else -> null
                    }
                }
            }
    }

    private fun handleIssueSelection(issueItem: IssueDisplayItem, commitMessage: CommitMessage) {
        // Store the selected issue for AI generation
        selectedIssue = issueItem

        val project = commitMessage.editorField.project ?: return
        val changes = currentChanges ?: return
        val event = currentEvent ?: return

        // Generate AI commit message with issue context
        generateAICommitMessage(project, commitMessage, changes, event)
    }

    private fun handleSkipIssueSelection(commitMessage: CommitMessage) {
        // Skip issue selection, generate with AI only
        selectedIssue = null

        val project = commitMessage.editorField.project ?: return
        val changes = currentChanges ?: return
        val event = currentEvent ?: return

        // Generate AI commit message without issue context
        generateAICommitMessage(project, commitMessage, changes, event)
    }

    private fun generateAICommitMessage(project: Project, commitMessage: CommitMessage, changes: List<Change>, event: AnActionEvent) {
        val diffContext = project.service<VcsPrompting>().prepareContext(changes)
        if (diffContext.isEmpty() || diffContext == "\n") {
            logger.warn("Diff context is empty or cannot get enough useful context.")
            AutoDevNotifications.notify(project, "Diff context is empty or cannot get enough useful context.")
            return
        }

        val editorField = commitMessage.editorField
        val originText = editorField.editor?.selectionModel?.selectedText ?: ""

        currentJob?.cancel()
        editorField.text = ""
        event.presentation.icon = AutoDevStatus.InProgress.icon

        ApplicationManager.getApplication().executeOnPooledThread {
            val prompt = generateCommitMessage(diffContext, project, originText)
            logger.info(prompt)

            try {
                val stream = LlmFactory.create(project).stream(prompt, "", false)
                currentJob = AutoDevCoroutineScope.scope(project).launch {
                    try {
                        stream.cancellable().collect { chunk ->
                            invokeLater {
                                if (isActive) {
                                    editorField.text += chunk
                                }
                            }
                        }

                        val text = editorField.text
                        if (isActive && text.startsWith("```") && text.endsWith("```")) {
                            invokeLater {
                                editorField.text = CodeFence.parse(text).text
                            }
                        } else if (isActive) {
                            invokeLater {
                                editorField.text = text.removePrefix("```\n").removeSuffix("```")
                            }
                        }
                    } catch (e: Exception) {
                        logger.error("Error during commit message generation", e)
                        invokeLater {
                            AutoDevNotifications.notify(project, "Error generating commit message: ${e.message}")
                        }
                    } finally {
                        invokeLater {
                            event.presentation.icon = AutoDevStatus.Ready.icon
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("Failed to start commit message generation", e)
                event.presentation.icon = AutoDevStatus.Error.icon
                AutoDevNotifications.notify(project, "Failed to start commit message generation: ${e.message}")
            }
        }
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

        val issue = selectedIssue?.issue
        val issueDetail = if (issue != null) {
            buildString {
                appendLine("Title: ${issue.title}")
                if (!issue.body.isNullOrBlank()) {
                    appendLine("Description: ${issue.body}")
                }
            }
        } else ""

        templateRender.context = CommitMsgGenContext(
            historyExamples = historyExamples,
            diffContent = diff,
            originText = originText,
            issueId = issue?.number?.toString() ?: "",
            issueDetail = issueDetail
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
    // GitHub issue information if selected
    val issueId: String = "",
    val issueDetail: String = "",
) : TemplateContext