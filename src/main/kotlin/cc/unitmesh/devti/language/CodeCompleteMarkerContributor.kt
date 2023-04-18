package cc.unitmesh.devti.language

import cc.unitmesh.devti.DevtiIcons
import cc.unitmesh.devti.prompt.openai.OpenAIExecutor
import cc.unitmesh.devti.runconfig.AutoCRUDState
import cc.unitmesh.devti.settings.DevtiSettingsState
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod

class CodeCompleteMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element !is PsiIdentifier) return null
        val method = element.parent
        if (method !is PsiMethod) return null

        val methodName = method.name

        val runAction = object : AnAction({ "Code Complete for $methodName" }, DevtiIcons.AI_COPILOT) {
            override fun actionPerformed(e: AnActionEvent) {
                val psiElementFactory = e.project?.let { JavaPsiFacade.getElementFactory(it) }

                ApplicationManager.getApplication().invokeLater {
                    val openAiVersion = DevtiSettingsState.getInstance()?.openAiVersion?: return@invokeLater
                    val openAiKey = DevtiSettingsState.getInstance()?.openAiKey?: return@invokeLater

                    val apiExecutor = OpenAIExecutor(openAiKey, openAiVersion)
                    val newMethodCode = apiExecutor.codeCompleteFor(method.text).trimIndent()

                    if (newMethodCode.isEmpty()) {
                        log.warn("no code complete result")
                    }

                    runWriteAction {
                        psiElementFactory?.createMethodFromText(newMethodCode, method.parent)?.let {
                            method.replace(it)
                        }
                    }
                }
            }
        }

        return Info(
            DevtiIcons.AI_COPILOT,
            { "Code Complete " },
            runAction
        )
    }

    companion object {
        private val log: Logger = logger<AutoCRUDState>()
    }
}
