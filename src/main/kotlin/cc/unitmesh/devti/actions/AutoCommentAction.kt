package cc.unitmesh.devti.actions

import cc.unitmesh.devti.DevtiIcons
import cc.unitmesh.devti.prompt.openai.OpenAIExecutor
import cc.unitmesh.devti.runconfig.AutoCRUDState
import cc.unitmesh.devti.settings.DevtiSettingsState
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiMethod

class AutoCommentAction(
    private val methodName: @NlsSafe String,
    private val method: PsiMethod
) : AnAction({ "Auto Comment for $methodName" }, DevtiIcons.AI_COPILOT) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val psiElementFactory = project.let { JavaPsiFacade.getElementFactory(it) }

        ApplicationManager.getApplication().invokeLater {
            // 1. get openai key and version
            val openAiVersion = DevtiSettingsState.getInstance()?.openAiVersion ?: return@invokeLater
            val openAiKey = DevtiSettingsState.getInstance()?.openAiKey ?: return@invokeLater

            // 2. get code complete result
            val apiExecutor = OpenAIExecutor(openAiKey, openAiVersion)
            val newMethodCode = apiExecutor.autoComment(method.text).trimIndent()

            if (newMethodCode.isEmpty()) {
                log.error("no code complete result")
                return@invokeLater
            }
            log.warn("newMethodCode: $newMethodCode")

            // 3. replace method
            WriteCommandAction.runWriteCommandAction(project) {
                psiElementFactory?.createMethodFromText(newMethodCode, method)?.let {
                    method.replace(it)
                }
            }
        }
    }

    companion object {
        private val log: Logger = logger<AutoCRUDState>()
    }
}