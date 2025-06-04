package cc.unitmesh.devti.mcp

import cc.unitmesh.devti.mcp.host.IssueArgs
import cc.unitmesh.devti.mcp.host.IssueEvaluateTool
import com.intellij.openapi.application.JBProtocolCommand
import com.intellij.openapi.project.ProjectManager

class AutoDevJbProtocolService : JBProtocolCommand("autodev") {
    override suspend fun execute(
        target: String?,
        parameters: Map<String, String>,
        fragment: String?
    ): String? {
        val issueEvaluateTool = IssueEvaluateTool()
        val issue = parameters["issue"]
        if (issue == null) {
            return null
        }

        val args: IssueArgs = IssueArgs(issue = issue)

        val project = ProjectManager.getInstance().openProjects.firstOrNull()
            ?: return null
        val result = issueEvaluateTool.handle(project, args)
        return result.toString()
    }
}