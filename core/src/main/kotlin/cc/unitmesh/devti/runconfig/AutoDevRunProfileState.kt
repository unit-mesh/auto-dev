package cc.unitmesh.devti.runconfig

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.flow.kanban.Kanban
import cc.unitmesh.devti.flow.kanban.impl.GitHubIssue
import cc.unitmesh.devti.flow.kanban.impl.GitLabIssue
import cc.unitmesh.devti.gui.sendToChatPanel
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.provider.DevFlowProvider
import cc.unitmesh.devti.runconfig.options.AutoCRUDConfigurationOptions
import cc.unitmesh.devti.settings.AutoDevSettingsState
import cc.unitmesh.devti.settings.devops.devopsPromptsSettings
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project

class AutoDevRunProfileState(
    val project: Project,
    val options: AutoCRUDConfigurationOptions,
) : RunProfileState {
    val state = project.devopsPromptsSettings.state
    private val githubToken: String = state.githubToken
    private val gitlabToken: String = state.gitlabToken
    private val gitType: String = state.gitType
    private val gitlabUrl: String = state.gitlabUrl

    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult? {
        val kanban: Kanban = when (gitType.lowercase()) {
            "gitlab" -> {
                GitLabIssue(options.githubRepo(), gitlabToken, gitlabUrl)
            }

            "github" -> {
                GitHubIssue(options.githubRepo(), githubToken)
            }

            else -> {
                GitHubIssue(options.githubRepo(), githubToken)
            }
        }

        // TODO: support other language
        val flowProvider = DevFlowProvider.flowProvider("java")
        if (flowProvider == null) {
            logger.error("current Language don't implementation DevFlow")
            return null
        }
        val openAIRunner = LlmFactory().create(project)

        sendToChatPanel(project) { contentPanel, _ ->
            flowProvider.initContext(kanban, openAIRunner, contentPanel, project)
            ProgressManager.getInstance().run(executeCrud(flowProvider))
        }

        return null
    }

    private fun executeCrud(flowProvider: DevFlowProvider) =
        object : Task.Backgroundable(project, "Loading retained test failure", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = false
                indicator.fraction = 0.0

                indicator.text = AutoDevBundle.message("autocrud.progress.creatingStory")

                // todo: check create story
                val storyId = options.storyId()
                val storyDetail = flowProvider.getOrCreateStoryDetail(storyId)

                indicator.fraction = 0.2

                indicator.text = AutoDevBundle.message("autocrud.generatingDtoAndEntity")
                flowProvider.updateOrCreateDtoAndEntity(storyDetail)

                indicator.fraction = 0.4

                indicator.text = AutoDevBundle.message("autocrud.progress.fetchingSuggestEndpoint")
                val target = flowProvider.fetchSuggestEndpoint(storyDetail)

                indicator.fraction = 0.6

                indicator.text = AutoDevBundle.message("autocrud.progress.updatingEndpointMethod")
                flowProvider.updateOrCreateEndpointCode(target, storyDetail)

                indicator.fraction = 0.8

                indicator.text = AutoDevBundle.message("autocrud.progress.creatingServiceAndRepository")
                flowProvider.updateOrCreateServiceAndRepository()

                indicator.fraction = 1.0
            }
        }

    companion object {
        private val logger: Logger = logger<AutoDevRunProfileState>()
    }
}