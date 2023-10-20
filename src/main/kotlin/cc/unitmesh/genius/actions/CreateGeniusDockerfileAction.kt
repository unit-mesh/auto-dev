package cc.unitmesh.genius.actions

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class CreateGeniusDockerfileAction : AnAction(
    AutoDevBundle.message("action.new.genius.dockerfile"),
    AutoDevBundle.message("action.new.genius.dockerfile"),
    AutoDevIcons.AI_COPILOT
) {
    override fun actionPerformed(e: AnActionEvent) {
        TODO("Not yet implemented")
    }
}
