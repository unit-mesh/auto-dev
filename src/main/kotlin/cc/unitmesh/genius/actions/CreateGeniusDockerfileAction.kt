package cc.unitmesh.genius.actions

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class CreateGeniusDockerfileAction : AnAction(AutoDevBundle.message("action.new.genius.dockerfile")) {
    override fun actionPerformed(e: AnActionEvent) {
        // call OpenAI and send to Bundle
    }
}
