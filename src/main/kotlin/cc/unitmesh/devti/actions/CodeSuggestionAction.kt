package cc.unitmesh.devti.actions

import cc.unitmesh.devti.DevtiIcons
import cc.unitmesh.devti.connector.openai.OpenCodeCopilot
import cc.unitmesh.devti.runconfig.AutoCRUDState
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.ui.EditorTextField
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JPanel


class CodeSuggestionAction(methodName: @NlsSafe String, val method: PsiMethod) : AnAction({ "Code Suggestion for $methodName" }, DevtiIcons.AI_COPILOT) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val task = object : Task.Backgroundable(project, "Code completing", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.fraction = 0.2
                indicator.text = "Preparing code complete prompt"

                val apiExecutor = OpenCodeCopilot()

                indicator.fraction = 0.5
                indicator.text = "Call OpenAI API..."

                val className = if (method.parent is PsiClass) {
                    (method.parent as PsiClass).name
                } else {
                    method.containingFile?.name?.replace(".java", "")
                }

                val suggestion = apiExecutor.codeReviewFor(method.text, className!!).trimIndent()

                indicator.fraction = 0.8
                indicator.text = "Start replacing method"

                val myTextField = EditorTextField(suggestion)

                val panel = JPanel(BorderLayout(0, 20))
                panel.add(myTextField, BorderLayout.CENTER)

                val builder = JBPopupFactory.getInstance().createComponentPopupBuilder(panel, myTextField)


                val popup = builder.createPopup()
                popup.setMinimumSize(Dimension(400, 20))
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

    companion object {
        private val log: Logger = logger<AutoCRUDState>()
    }
}
