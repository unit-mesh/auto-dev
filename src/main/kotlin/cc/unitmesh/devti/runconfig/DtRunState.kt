package cc.unitmesh.devti.runconfig

import cc.unitmesh.devti.DevtiFlow
import cc.unitmesh.devti.analysis.JavaCrudProcessor
import cc.unitmesh.devti.kanban.impl.GitHubIssue
import cc.unitmesh.devti.prompt.openai.OpenAIAction
import cc.unitmesh.devti.runconfig.config.DevtiCreateStoryConfigure
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project

class DtRunState(
    val environment: ExecutionEnvironment,
    private val configuration: DtRunConfiguration,
    private val createStory: DevtiCreateStoryConfigure?,
    val project: Project,
    val options: DtRunConfigurationOptions
) : RunProfileState {

    // check configuration
    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult? {
        val javaAuto = JavaCrudProcessor(project)
        val gitHubIssue = GitHubIssue("unit-mesh/untitled", options.githubToken())
        val openAIAction = OpenAIAction(options.openAiApiKey(), "gpt-3.5-turbo")
        val devtiFlow = DevtiFlow(gitHubIssue, openAIAction, javaAuto)

        log.warn(configuration.toString())
        log.warn(createStory.toString())
        log.warn(options.toString())

        // todo: check create story
        val storyId = createStory?.storyId ?: 1

        devtiFlow.start(storyId.toString())

        return null
    }

    companion object {
        private val log: Logger = logger<DtRunState>()
    }
}