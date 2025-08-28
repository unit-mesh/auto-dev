package cc.unitmesh.devti.actions

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.actions.context.DevOpsContext
import cc.unitmesh.devti.custom.tasks.FileGenerateTask
import cc.unitmesh.devti.provider.BuildSystemProvider
import cc.unitmesh.devti.template.GENIUS_CICD
import cc.unitmesh.devti.template.TemplateRender
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.guessProjectDir
import kotlin.io.path.createDirectories

class GenerateGitHubActionsAction : AnAction(AutoDevBundle.message("action.new.genius.cicd.github")) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        try {
            // 改进变量命名
            val buildSystem = BuildSystemProvider.guess(project)
            val templateRender = TemplateRender(GENIUS_CICD)
            templateRender.context = DevOpsContext.from(buildSystem)
            val template = templateRender.getTemplate("generate-github-action.vm")

            // 安全的路径处理
            val projectDir = project.guessProjectDir()?.toNioPath()
                ?: throw IllegalStateException("Cannot determine project directory")

            val workflowDir = projectDir.resolve(".github").resolve("workflows")
            workflowDir.createDirectories()

            val msgs = templateRender.buildMsgs(template)
            val task: Task.Backgroundable = FileGenerateTask(project, msgs, "ci.yml")

            ProgressManager.getInstance()
                .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))

        } catch (e: Exception) {
            // 添加错误处理，可以显示错误通知给用户
            logger<GenerateGitHubActionsAction>().error("Failed to generate GitHub Actions workflow", e)
            // 可以添加用户通知
        }
    }
}


