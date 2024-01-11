package cc.unitmesh.devti.actions.vcs

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.actions.chat.base.ChatBaseAction
import cc.unitmesh.devti.flow.kanban.impl.GitHubIssue
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.sendToChatPanel
import cc.unitmesh.devti.prompting.VcsPrompting
import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.devti.provider.context.ChatOrigin
import cc.unitmesh.devti.settings.AutoDevSettingsState
import cc.unitmesh.devti.template.TemplateRender
import com.intellij.dvcs.repo.Repository
import com.intellij.dvcs.repo.VcsRepositoryManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change
import com.intellij.vcs.log.VcsFullCommitDetails
import com.intellij.vcs.log.VcsLogDataKeys
import git4idea.repo.GitRepository
import kotlinx.coroutines.runBlocking
import org.changelog.CommitParser
import java.nio.file.FileSystems
import java.nio.file.PathMatcher

val githubUrlRegex: Regex = Regex("^(https?://|git://)?(www\\.)?github\\.com/[\\w-]+/[\\w-]+(/.*)?\$")

open class CodeReviewAction : ChatBaseAction() {
    override fun getActionType(): ChatActionType = ChatActionType.CODE_REVIEW

    private val commitParser: CommitParser = CommitParser()

    private val defaultIgnoreFilePatterns: List<PathMatcher> = listOf(
        "**/*.md", "**/*.json", "**/*.txt", "**/*.xml", "**/*.yml", "**/*.yaml",
    ).map {
        FileSystems.getDefault().getPathMatcher("glob:$it")
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        // Make changes available for diff action
        val vcsLog = event.getData(VcsLogDataKeys.VCS_LOG)
        val details: List<VcsFullCommitDetails> = vcsLog?.selectedDetails?.toList() ?: return
        val selectList = event.getData(VcsDataKeys.SELECTED_CHANGES) ?: return

        var stories: List<String> = listOf()
        ProgressManager.getInstance().runProcessWithProgressSynchronously(Runnable {
            val repositoryManager: VcsRepositoryManager = VcsRepositoryManager.getInstance(project)
            val repository = repositoryManager.getRepositoryForFile(project.baseDir)

            if (repository == null) {
                AutoDevNotifications.notify(project, "No git repository found.")
                return@Runnable
            }

            stories = fetchKanbanByCommits(repository, details)
        }, "Prepare Repository", true, project)

        doReviewWithChanges(project, details, selectList, stories)
    }

    fun doReviewWithChanges(
        project: Project,
        details: List<VcsFullCommitDetails>,
        selectList: Array<out Change>,
        stories: List<String>
    ) {
        val vcsPrompting = project.service<VcsPrompting>()
        val fullChangeContent =
            vcsPrompting.buildDiffPrompt(details, selectList.toList(), project, defaultIgnoreFilePatterns)

        if (fullChangeContent == null) {
            AutoDevNotifications.notify(project, "No code to review.")
            return
        }

        val creationContext =
            ChatCreationContext(ChatOrigin.Intention, getActionType(), null, listOf(), null)

        val contextItems: List<ChatContextItem> = runBlocking {
            return@runBlocking ChatContextProvider.collectChatContextList(project, creationContext)
        }

        val context = CodeReviewContext(
            frameworkContext = contextItems.joinToString("\n") { it.text },
            stories = stories.toMutableList(),
            diffContext = fullChangeContent,
        )

        doCodeReview(project, context)
    }

    fun doCodeReview(project: Project, context: CodeReviewContext) {
        val templateRender = TemplateRender("genius/practises")
        val template = templateRender.getTemplate("code-review.vm")
        templateRender.context = context
        val messages = templateRender.buildMsgs(template)

        log.info("messages: $messages")

        sendToChatPanel(project) { panel, service ->
            service.handleMsgsAndResponse(panel, messages)
        }
    }

    private fun fetchKanbanByCommits(repository: Repository, details: List<VcsFullCommitDetails>): List<String> {
        val stories: MutableList<String> = mutableListOf()
        when (repository) {
            is GitRepository -> {
                val remote = repository.info.remotes.firstOrNull() ?: return stories
                val url = remote.firstUrl ?: return stories
                if (!url.matches(githubUrlRegex)) return stories

                val github = GitHubIssue(url, AutoDevSettingsState.getInstance().githubToken)
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

        return stories
    }

    companion object {
        val log = logger<CodeReviewAction>()
    }
}

data class CodeReviewContext(
    var frameworkContext: String = "",
    val stories: MutableList<String> = mutableListOf(),
    var diffContext: String = "",
)