package cc.unitmesh.devti.runconfig

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.flow.AutoDevFlow
import cc.unitmesh.devti.flow.JavaCrudProcessor
import cc.unitmesh.devti.flow.kanban.impl.GitHubIssue
import cc.unitmesh.devti.connector.openai.OpenAIConnector
import cc.unitmesh.devti.runconfig.config.AutoDevConfiguration
import cc.unitmesh.devti.runconfig.options.AutoDevConfigurationOptions
import cc.unitmesh.devti.settings.AutoDevSettingsState
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project

class AutoDevRunProfileState(
    val environment: ExecutionEnvironment,
    private val configuration: AutoDevConfiguration,
    val project: Project,
    val options: AutoDevConfigurationOptions
) : RunProfileState {
    private val githubToken: String

    init {
        val instance = AutoDevSettingsState.getInstance()
        githubToken = instance?.githubToken ?: ""
    }

    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult? {
        val javaAuto = JavaCrudProcessor(project)
        val gitHubIssue = GitHubIssue(options.githubRepo(), githubToken)

        val openAIRunner = OpenAIConnector()
        val autoDevFlow = AutoDevFlow(gitHubIssue, openAIRunner, javaAuto)

        log.warn(configuration.toString())
        log.warn(options.toString())

        ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, "Loading retained test failure", true) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = false
                    indicator.fraction = 0.0

                    indicator.text = AutoDevBundle.message("devti.runconfig.progress.creatingStory")

                    // todo: check create story
                    val storyId = options.storyId()
                    val storyDetail = autoDevFlow.fillStoryDetail(storyId)

                    indicator.fraction = 0.3

                    indicator.text = AutoDevBundle.message("devti.runconfig.progress.fetchingSuggestEndpoint")
                    val target = autoDevFlow.fetchSuggestEndpoint(storyDetail)

                    indicator.fraction = 0.6

                    indicator.text = AutoDevBundle.message("devti.runconfig.progress.updatingEndpointMethod")
                    autoDevFlow.updateEndpointMethod(target, storyDetail)

                    indicator.fraction = 1.0
                }
            }
        )

        return null
    }

    companion object {
        private val log: Logger = logger<AutoDevRunProfileState>()
    }
}