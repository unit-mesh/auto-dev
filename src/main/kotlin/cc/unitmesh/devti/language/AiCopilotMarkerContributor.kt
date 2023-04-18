package cc.unitmesh.devti.language

import cc.unitmesh.devti.DevtiIcons
import cc.unitmesh.devti.runconfig.AutoCRUDState
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod

class AiCopilotMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element !is PsiIdentifier) return null
        val parent = element.parent
        if (parent !is PsiMethod) return null

        val runAction = object : AnAction({ "Auto Coding" }, DevtiIcons.AI_COPILOT) {
            override fun actionPerformed(e: AnActionEvent) {
                ApplicationManager.getApplication().invokeLater {
                    execute("devti://ai-copilot", DefaultRunExecutor.EXECUTOR_ID)
                }
            }
        }

        return Info(
            DevtiIcons.AI_COPILOT,
            { "Auto Coding" },
            runAction
        )
    }

    private fun execute(command: String, executorId: String) {
        log.warn("execute: $command, $executorId")
        runReadAction {
            // todo: modify files
        }
    }

    companion object {
        private val log: Logger = logger<AutoCRUDState>()
    }
}
