package cc.unitmesh.devti.actions

import cc.unitmesh.devti.DevtiIcons
import cc.unitmesh.devti.connector.ConnectorService
import cc.unitmesh.devti.connector.openai.OpenAIConnector
import cc.unitmesh.devti.gui.createSuggestionPopup
import cc.unitmesh.devti.runconfig.AutoCRUDState
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiMethod

class FindBugAction(methodName: @NlsSafe String, val method: PsiMethod) :
    AnAction({ "Find bug for $methodName" }, DevtiIcons.AI_COPILOT) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val code = method.text
        val apiExecutor = ConnectorService.getInstance().connector()

        val task = object : Task.Backgroundable(project, "Find bug", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.fraction = 0.2
                indicator.text = "Preparing code complete prompt"

                indicator.fraction = 0.5
                indicator.text = "Call OpenAI API..."

                val suggestion = apiExecutor.findBug(code).trimIndent()

                indicator.fraction = 0.8
                indicator.text = "Start replacing method"

                val popup = createSuggestionPopup(suggestion)
                ApplicationManager.getApplication().invokeLater() {
                    popup.showCenteredInCurrentWindow(project)
                }

                indicator.fraction = 1.0
            }
        }

        ApplicationManager.getApplication().invokeLater {
            ProgressManager.getInstance().run(task)
        }
    }
}
