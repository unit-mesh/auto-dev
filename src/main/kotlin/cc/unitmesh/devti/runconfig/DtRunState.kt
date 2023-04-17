package cc.unitmesh.devti.runconfig

import cc.unitmesh.devti.DevtiBundle
import cc.unitmesh.devti.DevtiFlow
import cc.unitmesh.devti.analysis.JavaCrudProcessor
import cc.unitmesh.devti.kanban.impl.GitHubIssue
import cc.unitmesh.devti.prompt.openai.OpenAIAction
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

class DtRunState(
    val environment: ExecutionEnvironment,
    private val configuration: DtRunConfiguration,
    val project: Project,
    val options: DtRunConfigurationOptions
) : RunProfileState {
    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult? {
        val javaAuto = JavaCrudProcessor(project)
        val gitHubIssue = GitHubIssue(options.githubRepo(), options.githubToken())
        val openAIAction = OpenAIAction(options.openAiApiKey(), "gpt-3.5-turbo")
        val devtiFlow = DevtiFlow(gitHubIssue, openAIAction, javaAuto)

        // check configuration

        log.warn(configuration.toString())
        log.warn(options.toString())

        ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, "Loading retained test failure", true) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = false
                    indicator.fraction = 0.0

                    indicator.text = DevtiBundle.message("devti.runconfig.progress.creatingStory")

                    // todo: check create story
                    val storyId = options.storyId()
                    val storyDetail = devtiFlow.fillStoryDetail(storyId)

                    indicator.fraction = 0.3

                    indicator.text = DevtiBundle.message("devti.runconfig.progress.fetchingSuggestEndpoint")
                    val target = devtiFlow.fetchSuggestEndpoint(storyDetail)
                    if (!target.hasMatchedController) {
                        log.warn("no suggest endpoint found, create new endpoint")
                        val newEndpoint = devtiFlow.createNewEndpoint(target.endpoint) ?: return

                        target.controller = newEndpoint
                        log.warn("created new endpoint: $newEndpoint")
                    }

                    indicator.fraction = 0.6

                    indicator.text = DevtiBundle.message("devti.runconfig.progress.updatingEndpointMethod")
                    devtiFlow.updateEndpointMethod(target, storyDetail)

                    indicator.fraction = 0.9
                }
            }
        )

        return null
    }

    companion object {
        private val log: Logger = logger<DtRunState>()
    }
}