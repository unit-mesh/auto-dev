package cc.unitmesh.idea.action

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.llms.ConnectorFactory
import cc.unitmesh.devti.llms.LLMCoroutineScopeService
import cc.unitmesh.devti.runconfig.AutoDevRunProfileState
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiMethod
import kotlinx.coroutines.launch

class AutoCommentAction(
    private val methodName: @NlsSafe String,
    private val method: PsiMethod
) : AnAction({ "Auto Comment for $methodName" }, AutoDevIcons.AI_COPILOT) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val apiExecutor = ConnectorFactory.getInstance().connector(project)

        val psiElementFactory = project.let {
            JavaPsiFacade.getElementFactory(it)
        }

        ApplicationManager.getApplication().invokeLater {
            LLMCoroutineScopeService.scope(project).launch {
                val newMethodCode = apiExecutor.autoComment(method.text).trimIndent()

                if (newMethodCode.isEmpty()) {
                    log.error("no code complete result")
                    return@launch
                }
                log.warn("newMethodCode: $newMethodCode")

                // 2. replace method
                WriteCommandAction.runWriteCommandAction(project) {
                    psiElementFactory?.createMethodFromText(newMethodCode, method)?.let {
                        method.replace(it)
                    }
                }
            }
        }
    }

    companion object {
        private val log: Logger = logger<AutoDevRunProfileState>()
    }
}