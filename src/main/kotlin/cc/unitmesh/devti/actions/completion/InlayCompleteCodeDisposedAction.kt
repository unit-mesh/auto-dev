package cc.unitmesh.devti.actions.completion

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.CommandProcessor
import com.intellij.temporary.inlay.codecomplete.InlayDisposeContext
import com.intellij.temporary.inlay.codecomplete.LLMInlayManager

/**
 * A quick insight action is an action that can be triggered by a user,
 * user can input custom text to call with LLM.
 */
class InlayCompleteCodeDisposedAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val dataContext = e.dataContext
        val editor = dataContext.getData(CommonDataKeys.EDITOR) ?: return
        val project = dataContext.getData(CommonDataKeys.PROJECT) ?: return
        if (project.isDisposed) return;

        val commandProcessor = CommandProcessor.getInstance()
        if (commandProcessor.isUndoTransparentActionInProgress) return

        val llmInlayManager = LLMInlayManager.getInstance()
        llmInlayManager.disposeInlays(editor, InlayDisposeContext.UserAction)
    }
}

