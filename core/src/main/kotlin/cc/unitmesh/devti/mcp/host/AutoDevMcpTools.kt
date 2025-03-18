package cc.unitmesh.devti.mcp.host

import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import com.intellij.openapi.project.Project
import kotlinx.serialization.Serializable
import com.intellij.openapi.application.runInEdt

@Serializable
data class IssueArgs(val issue: String)

class IssueEvaluateTool : AbstractMcpTool<IssueArgs>() {
    override val name: String = "issue_or_story_evaluate"
    override val description: String = """
        This tool is used to evaluate an issue or story in the context of the project.
        Requires a issue parameter containing the issue description.
        Returns a the plan about this issue.
    """.trimIndent()

    override fun handle(
        project: Project,
        args: IssueArgs
    ): Response {
        val issue = args.issue

        runInEdt {
            AutoDevToolWindowFactory.Companion.sendToSketchToolWindow(project, ChatActionType.SKETCH) { ui, _ ->
                ui.sendInput(issue)
            }
        }

        return Response("Start analysis in IDEA")
    }
}