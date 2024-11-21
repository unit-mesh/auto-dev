package cc.unitmesh.devti.actions

import cc.unitmesh.devti.custom.tasks.FileGenerateTask
import cc.unitmesh.devti.provider.BuildSystemProvider
import cc.unitmesh.devti.template.GENIUS_SRE
import cc.unitmesh.devti.template.TemplateRender
import cc.unitmesh.devti.actions.context.DevOpsContext
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator


class GenerateDockerfileAction : AnAction("Generate Dockerfile") {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val dockerContexts = BuildSystemProvider.guess(project)
        val templateRender = TemplateRender(GENIUS_SRE)
        templateRender.context = DevOpsContext.from(dockerContexts)
        val template = templateRender.getTemplate("generate-dockerfile.vm")

        val msgs = templateRender.buildMsgs(template)

        val task: Task.Backgroundable = FileGenerateTask(project, msgs, "Dockerfile", codeOnly = true)
        ProgressManager.getInstance()
            .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }
}

