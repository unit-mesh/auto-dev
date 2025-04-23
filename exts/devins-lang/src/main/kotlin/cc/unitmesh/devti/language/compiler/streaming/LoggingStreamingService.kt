package cc.unitmesh.devti.language.compiler.streaming

import cc.unitmesh.devti.gui.chat.message.ChatRole
import cc.unitmesh.devti.language.LLM_LOGGING
import cc.unitmesh.devti.language.LLM_LOGGING_JSONL
import cc.unitmesh.devti.language.ShireConstants
import cc.unitmesh.devti.language.console.DevInConsoleViewBase
import cc.unitmesh.devti.llms.custom.ChatMessage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * The `LoggingStreamingService` class is an implementation of the `StreamingServiceProvider` interface.
 * It provides functionality to log streaming data to a file within a project's directory.
 *
 * ### Properties:
 * - `name`: A string that represents the name of the streaming service, initialized to "logging".
 * - `result`: A private string that accumulates the streaming data received.
 */
class LoggingStreamingService : StreamingServiceProvider {
    private var outputDir: VirtualFile = LightVirtualFile()
    override var name: String = "logging"

    private var result: String = ""
    private var userPrompt: String = ""

    override fun onBeforeStreaming(project: Project, userPrompt: String, console: DevInConsoleViewBase?) {
        this.userPrompt = userPrompt
        this.outputDir = ShireConstants.outputDir(project) ?: throw IllegalStateException("Project directory not found")
        if (outputDir.findChild(LLM_LOGGING) == null) {
            ApplicationManager.getApplication().invokeAndWait {
                WriteAction.compute<VirtualFile, Throwable> {
                    outputDir.createChildData(this, LLM_LOGGING)
                }
            }
        } else {
            runInEdt {
                val file = outputDir.findChild(LLM_LOGGING)
                runWriteAction {
                    file?.setBinaryContent(ByteArray(0))
                }
            }
        }

        if (outputDir.findChild(LLM_LOGGING_JSONL) == null) {
            ApplicationManager.getApplication().invokeAndWait {
                WriteAction.compute<VirtualFile, Throwable> {
                    outputDir.createChildData(this, LLM_LOGGING_JSONL)
                }
            }
        }
    }

    override fun onStreaming(project: Project, flow: String, args: List<Any>) {
        result += flow

        val virtualFile = outputDir.findChild(LLM_LOGGING)
        val file = virtualFile?.path?.let { File(it) }
        file?.appendText(flow)
    }

    override fun afterStreamingDone(project: Project) {
        ApplicationManager.getApplication().invokeAndWait {
            WriteAction.compute<VirtualFile, Throwable> {
                val virtualFile = outputDir.createChildData(this, LLM_LOGGING_JSONL)
                val file = File(virtualFile.path)
                val value: List<ChatMessage> = listOf(
                    ChatMessage(ChatRole.User.name, userPrompt),
                    ChatMessage(ChatRole.System.name, result)
                )

                val result = Json.encodeToString<List<ChatMessage>>(value)

                file.appendText(result)
                file.appendText("\n")
                virtualFile
            }
        }
    }
}