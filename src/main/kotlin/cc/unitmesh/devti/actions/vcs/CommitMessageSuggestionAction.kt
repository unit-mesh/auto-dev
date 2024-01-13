package cc.unitmesh.devti.actions.vcs

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.actions.chat.base.ChatBaseAction
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.prompting.VcsPrompting
import cc.unitmesh.devti.statusbar.AutoDevStatus
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.ui.CommitMessage
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.collect

class CommitMessageSuggestionAction : ChatBaseAction() {
    val logger = logger<CommitMessageSuggestionAction>()

    override fun getActionType(): ChatActionType = ChatActionType.GEN_COMMIT_MESSAGE

    private var statusEventProducer: StatusEventProducer = StatusEventProducer()

    override fun update(e: AnActionEvent) {
        val data = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL)
        if (data == null) {
            e.presentation.icon = AutoDevStatus.WAITING.icon
            e.presentation.isEnabled = false
            return
        }

        val prompting = e.project?.service<VcsPrompting>()
        val changes: List<Change> = prompting?.hasChanges() ?: listOf()

        e.presentation.icon = AutoDevStatus.Ready.icon
        statusEventProducer.setOnEventListener(object : OnEventListener {
            override fun onEventOccurred(status: AutoDevStatus) {
                e.presentation.icon = status.icon
            }
        })
        e.presentation.isEnabled = changes.isNotEmpty()
    }

    override fun executeAction(event: AnActionEvent) {
        val project = event.project ?: return
        val diffContext = project.service<VcsPrompting>().prepareContext()
        if (diffContext.isEmpty() || diffContext == "\n") {
            logger.warn("Diff context is empty or cannot get enough useful context.")
            AutoDevNotifications.notify(project, "Diff context is empty or cannot get enough useful context.")
            return
        }

        val prompt = generateCommitMessage(diffContext)

        val commitMessageUi = event.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL)

        // empty commit message before generating
        (commitMessageUi as CommitMessage).editorField.text = ""

        logger.info("Start generating commit message.")
        logger.info(prompt)

        statusEventProducer.triggerEvent(AutoDevStatus.InProgress)
        val stream = LlmFactory().create(project).stream(prompt, "", false)

        ApplicationManager.getApplication().executeOnPooledThread() {
            runBlocking {
                stream.cancellable().collect {
                    invokeLater {
                        commitMessageUi.editorField.text += it
                    }
                }
            }

            statusEventProducer.triggerEvent(AutoDevStatus.Ready)
        }
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
- test(java): fix test case for user controller

Diff:

```diff
$diff
```

"""
    }


}
