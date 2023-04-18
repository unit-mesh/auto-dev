package cc.unitmesh.devti.language

import cc.unitmesh.devti.DevtiIcons
import cc.unitmesh.devti.prompt.openai.OpenAIExecutor
import cc.unitmesh.devti.runconfig.AutoCRUDState
import cc.unitmesh.devti.settings.DevtiSettingsState
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
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
                val project = e.project ?: return
                val psiElementFactory = project.let { JavaPsiFacade.getElementFactory(it) }

                ApplicationManager.getApplication().invokeLater {
                    val openAiVersion = DevtiSettingsState.getInstance()?.openAiVersion ?: return@invokeLater
                    val openAiKey = DevtiSettingsState.getInstance()?.openAiKey ?: return@invokeLater

                    val apiExecutor = OpenAIExecutor(openAiKey, openAiVersion)

                    val className = if (method.parent is PsiClass) {
                        (method.parent as PsiClass).name
                    } else {
                        method.containingFile?.name?.replace(".java", "")
                    }

                    val newMethodCode = apiExecutor.codeCompleteFor(method.text, className).trimIndent()

                    if (newMethodCode.isEmpty()) {
                        log.error("no code complete result")
                        return@invokeLater
                    }
                    log.warn("newMethodCode: $newMethodCode")

                    WriteCommandAction.runWriteCommandAction(project) {
                        psiElementFactory?.createMethodFromText(newMethodCode, method)?.let {
//                            method.body?.add(it)
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
