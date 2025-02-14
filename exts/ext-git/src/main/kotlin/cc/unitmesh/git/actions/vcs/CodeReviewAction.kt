package cc.unitmesh.git.actions.vcs

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.actions.chat.base.ChatBaseAction
import cc.unitmesh.devti.flow.kanban.impl.GitHubIssue
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.gui.sendToChatPanel
import cc.unitmesh.devti.vcs.VcsPrompting
import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.devti.provider.context.ChatOrigin
import cc.unitmesh.devti.settings.LanguageChangedCallback.presentationText
import cc.unitmesh.devti.settings.devops.devopsPromptsSettings
import cc.unitmesh.devti.template.GENIUS_PRACTISES
import cc.unitmesh.devti.template.TemplateRender
import cc.unitmesh.devti.template.context.TemplateContext
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

val githubUrlRegex: Regex = Regex("^(https?://|git://)?(www\\.)?github\\.com/[\\w-]+/[\\w-]+(/.*)?\$")

open class CodeReviewAction : ChatBaseAction() {

    init {
        presentationText("settings.autodev.others.codeReview", templatePresentation)
    }

    override fun getActionType(): ChatActionType = ChatActionType.CODE_REVIEW

    private val commitParser: CommitParser = CommitParser()

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

            stories = fetchKanbanByCommits(repository, details, project)
        }, "Prepare Repository", true, project)

        doReviewWithChanges(project, details, selectList, stories)
    }

    open fun doReviewWithChanges(
        project: Project,
        details: List<VcsFullCommitDetails>,
        selectList: Array<out Change>,
        stories: List<String>
    ) {
        val vcsPrompting = project.service<VcsPrompting>()
        val fullChangeContent =
            vcsPrompting.buildDiffPrompt(details, selectList.toList(), project)

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
        val templateRender = TemplateRender(GENIUS_PRACTISES)
        val template = templateRender.getTemplate("code-review.vm")
        templateRender.context = context
        val messages = templateRender.buildMsgs(template)

        log.info("messages: $messages")

        sendToChatPanel(project) { panel, service ->
            service.handleMsgsAndResponse(panel, messages)
        }
    }

    private fun fetchKanbanByCommits(
        repository: Repository,
        details: List<VcsFullCommitDetails>,
        project: Project
    ): List<String> {
        val stories: MutableList<String> = mutableListOf()
        val githubToken = project.devopsPromptsSettings.state.githubToken

        when (repository) {
            is GitRepository -> {
                val remote = repository.info.remotes.firstOrNull() ?: return stories
                val url = remote.firstUrl ?: return stories
                if (!url.matches(githubUrlRegex)) return stories

                val github = GitHubIssue(url, githubToken)
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
) : TemplateContext