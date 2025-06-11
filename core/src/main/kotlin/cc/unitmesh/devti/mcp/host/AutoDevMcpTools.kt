package cc.unitmesh.devti.mcp.host

import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.observer.agent.AgentStateService
import cc.unitmesh.devti.observer.plan.reviewPlan
import cc.unitmesh.devti.provider.AutoTestService
import cc.unitmesh.devti.sketch.AutoSketchMode
import cc.unitmesh.devti.sketch.AutoSketchModeListener
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import kotlinx.serialization.Serializable
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiManager
import java.util.concurrent.CompletableFuture

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
            AutoSketchMode.getInstance(project).isEnable = true
            AutoDevToolWindowFactory.Companion.sendToSketchToolWindow(project, ChatActionType.SKETCH) { ui, _ ->
                ui.sendInput(issue)
            }
        }

        val hintDisposable = Disposer.newDisposable()
        val future = CompletableFuture<String>()
        val connection = ApplicationManager.getApplication().messageBus.connect(hintDisposable)
        connection.subscribe(AutoSketchModeListener.TOPIC, object : AutoSketchModeListener {
            override fun start() {
                /// do nothing
            }

            override fun done() {
                val messages = project.getService(AgentStateService::class.java).getAllMessages()
                var plan = ""
                messages.lastOrNull()?.content?.also {
                    plan = CodeFence.parseAll(it).firstOrNull {
                        it.originLanguage == "plan"
                    }?.text ?: ""
                }

                if (plan.isNotEmpty()) {
                    future.complete(plan)
                } else {
                    val messages = project.getService(AgentStateService::class.java).getAllMessages()
                    if (messages.isNotEmpty()) {
                        val plan = reviewPlan(project, isBlockingMode = true)
                        future.complete(plan)
                    } else {
                        future.completeExceptionally(throw Exception("Failure to analysis"))
                    }
                }
            }
        })

        return Response(future.get())
    }
}

@Serializable
data class CreateTestForFileArgs(@McpParam(description = "file_name for create test") val fileName: String)

class CreateTestForFileTool : AbstractMcpTool<CreateTestForFileArgs>() {
    override val name: String = "create_test_for_file"
    override val description: String = """
        This tool is used to create a test for a file.
        Requires a file_name parameter containing the file name.
        Returns a the test code for this file.
    """.trimIndent()

    override fun handle(
        project: Project,
        args: CreateTestForFileArgs
    ): Response {
        val fileName = args.fileName
        val file = project.baseDir.findFileByRelativePath(fileName)
            ?: return Response(error = "File not found")
        val psiFile = PsiManager.getInstance(project).findFile(file)
            ?: return Response(error = "Current IDE don't support this file type")
        val context = AutoTestService.context(psiFile)
            ?: return Response(error = "AutoDev don't support this file type")

        val result = context.runFileAsync(project, file, psiFile)
            ?: return Response(error = "Failed to create test for file")

        return Response(result)
    }
}
