package cc.unitmesh.genius.actions

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.custom.tasks.FileGenerateTask
import cc.unitmesh.devti.provider.BuildSystemProvider
import cc.unitmesh.devti.template.TemplateRender
import cc.unitmesh.genius.actions.context.DevOpsContext
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator


class GenerateGitHubActionsAction : AnAction(AutoDevBundle.message("action.new.genius.cicd.github")) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // first, we need to guess language
        val dockerContexts = BuildSystemProvider.guess(project);
        val templateRender = TemplateRender("genius/cicd")
        templateRender.context = DevOpsContext.from(dockerContexts)

        val template = templateRender
            .getTemplate("create-dockerfile.vm")

        val msgs = templateRender.create(template)

        val task: Task.Backgroundable = FileGenerateTask(project, msgs, DOCKERFILE)
        ProgressManager.getInstance()
            .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }
}


