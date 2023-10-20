package cc.unitmesh.genius.actions

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.llms.LlmProviderFactory
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking

class CreateGeniusDockerfileAction : AnAction(AutoDevBundle.message("action.new.genius.dockerfile")) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val stream =
            LlmProviderFactory().connector(project).stream("TODO", "")

        var result = ""

        runBlocking {
            stream.cancellable().collect {
                result += it
            }
        }

//        executeCrud(project, result)
    }

    private fun executeCrud(project: Project, content: Any) {
        WriteCommandAction.runWriteCommandAction(project, "Living Document", "cc.unitmesh.livingDoc", {

        });
    }
}
