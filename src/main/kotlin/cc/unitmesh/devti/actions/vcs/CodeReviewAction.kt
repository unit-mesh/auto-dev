package cc.unitmesh.devti.actions.vcs

import cc.unitmesh.devti.actions.chat.base.ChatBaseAction
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.chat.ChatContext
import cc.unitmesh.devti.gui.sendToChatPanel
import cc.unitmesh.devti.prompting.VcsPrompting
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diff.impl.patch.IdeaTextPatchBuilder
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ContentRevision
import com.intellij.openapi.vcs.changes.CurrentContentRevision
import com.intellij.vcs.log.VcsLogDataKeys
import org.jetbrains.annotations.NotNull

class CodeReviewAction : ChatBaseAction() {
    override fun getActionType(): ChatActionType = ChatActionType.CODE_REVIEW

    companion object {
        val log = logger<CodeReviewAction>()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // Make changes available for diff action
        val vcsLog = e.getData(VcsLogDataKeys.VCS_LOG)
        val details = vcsLog?.let { log ->
            log.selectedDetails
        }?.toList() ?: return

        val vcsPrompting = project.service<VcsPrompting>()
        val diff = vcsPrompting.calculateDiff(details, project)

        var prompt = """You are a seasoned software developer, and I'm seeking your expertise to review the following code:
            |
            |- Please provide an overview of the business objectives and the context behind this commit. This will ensure that the code aligns with the project's requirements and goals.
            |- Focus on critical algorithms, logical flow, and design decisions within the code. Discuss how these changes impact the core functionality and the overall structure of the code.
            |- Identify and highlight any potential issues or risks introduced by these code changes. This will help reviewers pay special attention to areas that may require improvement or further analysis.
            |- Emphasize the importance of compatibility and consistency with the existing codebase. Ensure that the code adheres to the established standards and practices for code uniformity and long-term maintainability.
            |- Lastly, provide a concise high-level summary that encapsulates the key aspects of this commit. This summary should enable reviewers to quickly grasp the major changes in this update.
            |
            |PS: Your insights and feedback are invaluable in ensuring the quality and reliability of this code. Thank you for your assistance.
            |
        """.trimMargin()

        prompt += diff.second

        log.info("prompt: $prompt")

        sendToChatPanel(project) { panel, service ->
            val chatContext = ChatContext(null, "", "")

            service.handlePromptAndResponse(panel, object : ContextPrompter() {
                override fun displayPrompt() = prompt
                override fun requestPrompt() = prompt
            }, chatContext)
        }
    }
}
