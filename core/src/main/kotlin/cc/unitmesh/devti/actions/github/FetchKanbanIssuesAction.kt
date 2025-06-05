// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cc.unitmesh.devti.actions.github

import cc.unitmesh.devti.flow.kanban.impl.GitHubIssue
import com.intellij.ide.TextCopyProvider
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys.COPY_PROVIDER
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.ui.CommitMessage
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.speedSearch.SpeedSearchUtil.applySpeedSearchHighlighting
import com.intellij.util.containers.nullize
import com.intellij.util.ui.JBUI.scale
import org.kohsuke.github.GHIssue
import org.kohsuke.github.GHIssueState
import java.awt.Point
import javax.swing.JList
import javax.swing.ListSelectionModel.SINGLE_SELECTION

class FetchKanbanIssuesAction : DumbAwareAction() {
    data class IssueDisplayItem(val issue: GHIssue, val displayText: String)

    init {
        isEnabledInModalContext = true
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.text = "Show GitHub Issues"
        e.presentation.description = "Show and select GitHub issues from current repository"
        e.presentation.isVisible = project != null && GitHubIssue.isGitHubRepository(project)
        e.presentation.isEnabled = e.presentation.isVisible
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project!!
        val commitMessage = getCommitMessage(e)!!
        val task = object : Task.Backgroundable(project, "Loading GitHub Issues", true) {
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
                            Messages.showInfoMessage(
                                project,
                                "No issues found in this GitHub repository.",
                                "GitHub Issues"
                            )
                        } else {
                            createIssuesPopup(project, commitMessage, issues).showInBestPositionFor(e.dataContext)
                        }
                    }
                } catch (ex: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(project, "Failed to fetch GitHub issues: ${ex.message}", "GitHub Issues Error")
                    }
                }
            }
        }

        ProgressManager.getInstance().run(task)
    }

    private fun getCommitMessage(e: AnActionEvent) = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL) as? CommitMessage

    private fun fetchGitHubIssues(project: Project): List<IssueDisplayItem> {
        val ghRepository = GitHubIssue.parseGitHubRepository(project) ?: throw IllegalStateException("Not a GitHub repository")
        return ghRepository.getIssues(GHIssueState.OPEN).map { issue ->
            val displayText = "#${issue.number} - ${issue.title}"
            IssueDisplayItem(issue, displayText)
        }
    }

    private fun createIssuesPopup(project: Project, commitMessage: CommitMessage, issues: List<IssueDisplayItem>): JBPopup {
        var chosenIssue: IssueDisplayItem? = null
        var selectedIssue: IssueDisplayItem? = null

        return JBPopupFactory.getInstance().createPopupChooserBuilder(issues)
            .setTitle("Select Issue")
            .setVisibleRowCount(10)
            .setSelectionMode(SINGLE_SELECTION)
            .setItemSelectedCallback { selectedIssue = it }
            .setItemChosenCallback {
                commitMessage.setCommitMessage(it.displayText)
                commitMessage.editorField.selectAll()
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
                    chosenIssue?.let { issue ->
                        handleIssueSelection(project, issue)
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
                            override fun getTextLinesToCopy() =  listOfNotNull(selectedIssue?.displayText).nullize()
                        }
                        else -> null
                    }
                }
            }
    }

    private fun handleIssueSelection(project: Project, issueItem: IssueDisplayItem) {
        val issue = issueItem.issue
        val message = buildString {
            appendLine("Selected Issue: #${issue.number}")
            appendLine("Title: ${issue.title}")
            appendLine("State: ${issue.state}")
            appendLine("Author: ${issue.user?.login ?: "Unknown"}")

            issue.assignees?.let { assignees ->
                if (assignees.isNotEmpty()) {
                    appendLine("Assignees: ${assignees.joinToString(", ") { it.login }}")
                }
            }

            val labels = issue.labels.map { it.name }
            if (labels.isNotEmpty()) {
                appendLine("Labels: ${labels.joinToString(", ")}")
            }

            issue.milestone?.let { milestone ->
                appendLine("Milestone: ${milestone.title}")
            }

            appendLine("URL: ${issue.htmlUrl}")

            if (!issue.body.isNullOrBlank()) {
                appendLine("\nDescription:")
                appendLine(issue.body)
            }
        }

        Messages.showInfoMessage(project, message, "GitHub Issue Details")
    }
}