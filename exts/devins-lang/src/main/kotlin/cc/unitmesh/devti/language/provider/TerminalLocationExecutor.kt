package cc.unitmesh.devti.language.provider

import cc.unitmesh.devti.language.provider.terminal.TerminalHandler
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import java.awt.Component

interface TerminalLocationExecutor {
    fun getComponent(e: AnActionEvent): Component?
    fun bundler(project: Project, userInput: String): TerminalHandler?

    companion object {
        private val EP_NAME: ExtensionPointName<TerminalLocationExecutor> =
            ExtensionPointName.create("cc.unitmesh.shireTerminalExecutor")

        fun provide(project: Project): TerminalLocationExecutor? {
            return EP_NAME.extensionList.firstOrNull()
        }
    }
}
