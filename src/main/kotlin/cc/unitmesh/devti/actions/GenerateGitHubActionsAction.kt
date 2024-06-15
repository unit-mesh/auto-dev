package cc.unitmesh.devti.actions

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.custom.tasks.FileGenerateTask
import cc.unitmesh.devti.provider.BuildSystemProvider
import cc.unitmesh.devti.template.GENIUS_CICD
import cc.unitmesh.devti.template.TemplateRender
import cc.unitmesh.devti.actions.context.DevOpsContext
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.guessProjectDir
import kotlin.io.path.createDirectories

/**
 * The `GenerateGitHubActionsAction` class is a Kotlin class that extends the `AnAction` class. It represents an action that can be performed in the IntelliJ IDEA IDE to generate GitHub Actions for Continuous Integration and Continuous Deployment (CI/CD) workflows.
 *
 * This action is triggered when the user selects the option to generate GitHub Actions from the IDE's menu. It performs the following steps:
 * 1. Retrieves the current project from the `AnActionEvent` object.
 * 2. Guesses the build system used in the project by calling the `BuildSystemProvider.guess()` method.
 * 3. Initializes a `TemplateRender` object with the template path "genius/cicd".
 * 4. Creates a `DevOpsContext` object from the guessed build system and sets it as the context for the `TemplateRender` object.
 * 5. Retrieves the template named "generate-github-action.vm" from the `TemplateRender` object.
 * 6. Resolves the path to the `.github/workflows` directory in the project and creates it if it doesn't exist.
 * 7. Builds the messages required for the template rendering by calling the `buildMsgs()` method of the `TemplateRender` object.
 * 8. Creates a `FileGenerateTask` object with the project, messages, and output file name "ci.yml".
 * 9. Runs the `FileGenerateTask` asynchronously with a progress indicator using the `ProgressManager.getInstance().runProcessWithProgressAsynchronously()` method.
 *
 * This class does not return any code directly. It is responsible for triggering the generation of GitHub Actions and handling the progress of the generation process.
 *
 * @constructor Creates a new instance of the `GenerateGitHubActionsAction` class.
 * @extends AnAction
 *
 * @param name The name of the action, which is displayed in the IDE's menu.
 */
class GenerateGitHubActionsAction : AnAction(AutoDevBundle.message("action.new.genius.cicd.github")) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // first, we need to guess language
        val githubActions = BuildSystemProvider.guess(project);
        val templateRender = TemplateRender(GENIUS_CICD)
        templateRender.context = DevOpsContext.from(githubActions)
        val template = templateRender.getTemplate("generate-github-action.vm")

        project.guessProjectDir()!!.toNioPath().resolve(".github").resolve("workflows")
            .createDirectories()

        val msgs = templateRender.buildMsgs(template)

        val task: Task.Backgroundable = FileGenerateTask(project, msgs, "ci.yml")
        ProgressManager.getInstance()
            .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }
}


