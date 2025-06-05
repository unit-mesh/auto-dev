// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cc.unitmesh.devti.actions.github

import cc.unitmesh.devti.flow.kanban.impl.GitHubIssue
import com.intellij.icons.AllIcons
import com.intellij.ide.TextCopyProvider
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys.COPY_PROVIDER
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.speedSearch.SpeedSearchUtil.applySpeedSearchHighlighting
import com.intellij.util.containers.nullize
import org.kohsuke.github.GHIssue
import org.kohsuke.github.GHIssueState
import javax.swing.JList
import javax.swing.ListSelectionModel.SINGLE_SELECTION

class ShowGitHubIssuesAction : DumbAwareAction() {
    data class IssueDisplayItem(val issue: GHIssue, val displayText: String)

    init {
        isEnabledInModalContext = true
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.text = "Show GitHub Issues"
        e.presentation.description = "Show and select GitHub issues from current repository"
        
        e.presentation.isVisible = project != null && isGitHubProject(project)
        e.presentation.isEnabled = e.presentation.isVisible
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project!!
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val issues = fetchGitHubIssues(project)
                if (issues.isEmpty()) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showInfoMessage(
                            project,
                            "No issues found in this GitHub repository.",
                            "GitHub Issues"
                        )
                    }
                    return@executeOnPooledThread
                }
                
                ApplicationManager.getApplication().invokeLater {
                    createIssuesPopup(project, issues).showInBestPositionFor(e.dataContext)
                }
            } catch (ex: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog(
                        project,
                        "Failed to fetch GitHub issues: ${ex.message}",
                        "GitHub Issues Error"
                    )
                }
            }
        }
    }

    private fun isGitHubProject(project: Project): Boolean {
        return try {
            GitHubIssue.parseGitHubRepository(project) != null
        } catch (e: Exception) {
            false
        }
    }

    private fun fetchGitHubIssues(project: Project): List<IssueDisplayItem> {
        val ghRepository = GitHubIssue.parseGitHubRepository(project) 
            ?: throw IllegalStateException("Not a GitHub repository")
        
        val repoUrl = "${ghRepository.ownerName}/${ghRepository.name}"

        val repository = GitHubIssue.createGitHubConnection(project).getRepository(repoUrl)
        val issues = repository.getIssues(GHIssueState.OPEN).toList()
        
        return issues.map { issue ->
            val displayText = "#${issue.number} - ${issue.title}"
            IssueDisplayItem(issue, displayText)
        }
    }

    private fun createIssuesPopup(project: Project, issues: List<IssueDisplayItem>): JBPopup {
        var chosenIssue: IssueDisplayItem? = null
        var selectedIssue: IssueDisplayItem? = null

        return JBPopupFactory.getInstance().createPopupChooserBuilder(issues)
            .setTitle("Select GitHub Issue")
            .setVisibleRowCount(10)
            .setSelectionMode(SINGLE_SELECTION)
            .setItemSelectedCallback { selectedIssue = it }
            .setItemChosenCallback { chosenIssue = it }
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
                override fun onClosed(event: LightweightWindowEvent) {
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