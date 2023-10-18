package cc.unitmesh.devti.actions.vcs

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.actions.chat.base.ChatBaseAction
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.chat.ChatContext
import cc.unitmesh.devti.gui.sendToChatPanel
import cc.unitmesh.devti.prompting.VcsPrompting
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.vcs.log.VcsFullCommitDetails
import com.intellij.vcs.log.VcsLogDataKeys
import java.nio.file.FileSystems
import java.nio.file.PathMatcher

class CodeReviewAction : ChatBaseAction() {
    override fun getActionType(): ChatActionType = ChatActionType.CODE_REVIEW

    companion object {
        val log = logger<CodeReviewAction>()
    }

    val defaultIgnoreFilePatterns: List<PathMatcher> = listOf(
        "**/*.md", "**/*.json", "**/*.txt", "**/*.xml", "**/*.yml", "**/*.yaml",
    ).map {
        FileSystems.getDefault().getPathMatcher("glob:$it")
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // Make changes available for diff action
        val vcsLog = e.getData(VcsLogDataKeys.VCS_LOG)
        val details: List<VcsFullCommitDetails> = vcsLog?.selectedDetails?.toList() ?: return

        val vcsPrompting = project.service<VcsPrompting>()
        val diff = vcsPrompting.buildDiffPrompt(details, project, defaultIgnoreFilePatterns)

        if (diff == null) {
            AutoDevNotifications.notify(project, "No code to review.")
            return
        }

        var prompt =
            """You are a seasoned software developer, and I'm seeking your expertise to review the following code:
            |
            |- Focus on critical algorithms, logical flow, and design decisions within the code. Discuss how these changes impact the core functionality and the overall structure of the code.
            |- Identify and highlight any potential issues or risks introduced by these code changes. This will help reviewers pay special attention to areas that may require improvement or further analysis.
            |- Emphasize the importance of compatibility and consistency with the existing codebase. Ensure that the code adheres to the established standards and practices for code uniformity and long-term maintainability.
            |
        """.trimMargin()

        prompt += diff.second

        prompt += """As your Tech lead, I am only concerned with key code review issues. Please provide me with a critical summary. 
            | Submit your summary under 7 sentences in here:"""
            .trimMargin()

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
