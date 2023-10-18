package cc.unitmesh.devti.actions.vcs

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.actions.chat.base.ChatBaseAction
import cc.unitmesh.devti.flow.kanban.impl.GitHubIssue
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.chat.ChatContext
import cc.unitmesh.devti.gui.sendToChatPanel
import cc.unitmesh.devti.prompting.VcsPrompting
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.settings.AutoDevSettingsState
import com.intellij.dvcs.repo.Repository
import com.intellij.dvcs.repo.VcsRepositoryManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.vcs.log.VcsFullCommitDetails
import com.intellij.vcs.log.VcsLogDataKeys
import git4idea.repo.GitRepository
import org.changelog.CommitParser
import java.nio.file.FileSystems
import java.nio.file.PathMatcher


val githubUrlRegex: Regex = Regex("^(https?://|git://)?(www\\.)?github\\.com/[\\w-]+/[\\w-]+(/.*)?\$")


class CodeReviewAction : ChatBaseAction() {
    override fun getActionType(): ChatActionType = ChatActionType.CODE_REVIEW

    private val commitParser: CommitParser = CommitParser()

    companion object {
        val log = logger<CodeReviewAction>()
    }

    val defaultIgnoreFilePatterns: List<PathMatcher> = listOf(
        "**/*.md", "**/*.json", "**/*.txt", "**/*.xml", "**/*.yml", "**/*.yaml",
    ).map {
        FileSystems.getDefault().getPathMatcher("glob:$it")
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        val repositoryManager: VcsRepositoryManager = VcsRepositoryManager.getInstance(project)
        val repository = repositoryManager.getRepositoryForFile(project.baseDir)
        if (repository == null) {
            AutoDevNotifications.notify(project, "No git repository found.")
            return
        }

        // Make changes available for diff action
        val vcsLog = event.getData(VcsLogDataKeys.VCS_LOG)
        val details: List<VcsFullCommitDetails> = vcsLog?.selectedDetails?.toList() ?: return

        val stories: List<String> = fetchKanbanByCommits(repository, details)

        val vcsPrompting = project.service<VcsPrompting>()
        val diff = vcsPrompting.buildDiffPrompt(details, project, defaultIgnoreFilePatterns)

        if (diff == null) {
            AutoDevNotifications.notify(project, "No code to review.")
            return
        }

        var prompt =
            """You are a seasoned software developer, and I'm seeking your expertise to review the following code:
            |
            |- Focus on critical algorithms, logical flow, and design decisions within the code. Discuss how these changes impact the core functionality and the overall structure of the code.
            |- Identify and highlight any potential issues or risks introduced by these code changes. This will help reviewers pay special attention to areas that may require improvement or further analysis.
            |- Emphasize the importance of compatibility and consistency with the existing codebase. Ensure that the code adheres to the established standards and practices for code uniformity and long-term maintainability.
            |
        """.trimMargin()

        if (stories.isNotEmpty()) {
            prompt += "The following user stories are related to these changes:\n"
            prompt += stories.joinToString("\n")
            prompt += "\n"
        }

        prompt += diff.second

        prompt += """As your Tech lead, I am only concerned with key code review issues. Please provide me with a critical summary. 
            | Submit your summary under 7 sentences in here:"""
            .trimMargin()

        log.info("prompt: $prompt")

        sendToChatPanel(project) { panel, service ->
            val chatContext = ChatContext(null, "", "")

            service.handlePromptAndResponse(panel, object : ContextPrompter() {
                override fun displayPrompt() = prompt
                override fun requestPrompt() = prompt
            }, chatContext)
        }
    }

    private fun fetchKanbanByCommits(repository: Repository, details: List<VcsFullCommitDetails>): List<String> {
        val stories: MutableList<String> = mutableListOf()
        when (repository) {
            is GitRepository -> {
                // check repository.presentableUrl is a github  url
                val presentableUrl = repository.presentableUrl
                if (presentableUrl.matches(githubUrlRegex)) {
                    val github = GitHubIssue(presentableUrl, AutoDevSettingsState.getInstance().githubToken)
                    details
                        .map {
                            commitParser.parse(it.subject).references
                        }
                        .flatten()
                        .forEach {
                            val simpleStory = github.getStoryById(it.issue)
                            stories += simpleStory.title
                        }
                }
            }
        }

        return stories
    }
}
