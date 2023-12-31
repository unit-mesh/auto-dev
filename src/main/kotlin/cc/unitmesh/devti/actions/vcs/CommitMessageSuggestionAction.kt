package cc.unitmesh.devti.actions.vcs

import cc.unitmesh.devti.actions.chat.base.ChatBaseAction
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.chat.ChatContext
import cc.unitmesh.devti.gui.sendToChatPanel
import cc.unitmesh.devti.prompting.VcsPrompting
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change

class CommitMessageSuggestionAction : ChatBaseAction() {
    override fun getActionType(): ChatActionType = ChatActionType.GEN_COMMIT_MESSAGE

    override fun update(e: AnActionEvent) {
        val prompting = e.project?.service<VcsPrompting>()

        val data = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL)
        val changes: List<Change> = prompting?.hasChanges() ?: listOf()

        e.presentation.isEnabled = data != null && changes.isNotEmpty()
    }

    override fun executeAction(event: AnActionEvent) {
        val project = event.project ?: return
        val prompt = generateCommitMessage(prepareVcsContext())

        sendToChatPanel(project) { panel, service ->
            val chatContext = ChatContext(
                getReplaceableAction(event),
                prompt,
                ""
            )

            service.handlePromptAndResponse(panel, object : ContextPrompter() {
                override fun displayPrompt(): String = prompt
                override fun requestPrompt(): String = prompt
            }, chatContext, newChatContext = true)
        }
    }

    private fun prepareVcsContext(): String {
        val project = ProjectManager.getInstance().openProjects.firstOrNull() ?: return ""
        val prompting = project.service<VcsPrompting>()

        return prompting.prepareContext()
    }

    private fun generateCommitMessage(diff: String): String {
        return """Write a cohesive yet descriptive commit message for a given diff. 
Make sure to include both information What was changed and Why.
Start with a short sentence in imperative form, no more than 50 characters long.
Then leave an empty line and continue with a more detailed explanation, if necessary.
Explanation should have less than 200 characters.

examples:
- fix(authentication): add password regex pattern
- feat(storage): add new test cases

Diff:

```diff
$diff
```

"""
    }


}
