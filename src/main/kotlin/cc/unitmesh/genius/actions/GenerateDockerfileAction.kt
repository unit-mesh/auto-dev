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
import com.intellij.openapi.project.guessProjectDir


const val DOCKERFILE = "Dockerfile"

class GenerateDockerfileAction : AnAction(AutoDevBundle.message("action.new.genius.dockerfile")) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val dockerContexts = BuildSystemProvider.guess(project)
        val templateRender = TemplateRender("genius/sre")
        templateRender.context = DevOpsContext.from(dockerContexts)
        val template = templateRender.getTemplate("generate-dockerfile.vm")

        val msgs = templateRender.buildMsgs(template)

        val fileDir = project.guessProjectDir()!!.toNioPath().resolve(DOCKERFILE).toFile()
        if (!fileDir.exists()) {
            fileDir.createNewFile()
        }

        val task: Task.Backgroundable = FileGenerateTask(project, msgs, fileDir)
        ProgressManager.getInstance()
            .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }
}


