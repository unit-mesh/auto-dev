package cc.unitmesh.devti.actions

import cc.unitmesh.devti.DevtiIcons
import cc.unitmesh.devti.connector.ConnectorService
import cc.unitmesh.devti.connector.openai.OpenAIConnector
import cc.unitmesh.devti.runconfig.AutoCRUDState
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod

class CodeCompleteAction(
    private val methodName: @NlsSafe String,
    private val method: PsiMethod
) : AnAction({ "Code Complete for $methodName" }, DevtiIcons.AI_COPILOT) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val psiElementFactory = project.let { JavaPsiFacade.getElementFactory(it) }
        val code = method.text
        val apiExecutor = ConnectorService.getInstance().connector()

        val task = object : Task.Backgroundable(project, "Code completing", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.fraction = 0.5
                indicator.text = "Call OpenAI API..."

                val className = if (method.parent is PsiClass) {
                    (method.parent as PsiClass).name
                } else {
                    method.containingFile?.name?.replace(".java", "")
                }

                val newMethodCode = apiExecutor.codeCompleteFor(code, className!!).trimIndent()

                indicator.fraction = 0.8
                indicator.text = "Start replacing method"

                if (newMethodCode.isEmpty()) {
                    log.error("no code complete result")
                    return
                }
                log.warn("newMethodCode: $newMethodCode")

                WriteCommandAction.runWriteCommandAction(project) {
                    psiElementFactory?.createMethodFromText(newMethodCode, method)?.let {
                        method.replace(it)
                    }
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