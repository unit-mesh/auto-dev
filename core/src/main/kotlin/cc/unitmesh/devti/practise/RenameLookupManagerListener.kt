package cc.unitmesh.devti.practise

import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.settings.coder.coderSetting
import cc.unitmesh.devti.statusbar.AutoDevStatus
import cc.unitmesh.devti.statusbar.AutoDevStatusService
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.*
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiEditorUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

// TODO: spike why TypeScript not trigger this listener when rename a class, function, but Java does
class RenameLookupManagerListener(val project: Project) : LookupManagerListener {
    private val llm = LlmFactory.instance.create(project)
    private val logger = logger<RenameLookupManagerListener>()

    override fun activeLookupChanged(oldLookup: Lookup?, newLookup: Lookup?) {
        if (!project.coderSetting.state.enableRenameSuggestion) return

        val lookupImpl = newLookup as? LookupImpl ?: return
        val editor = lookupImpl.editor as? EditorEx ?: return
        val targetElement: PsiElement = lookupElement(lookupImpl, editor) ?: return

        // maybe user just typing, we should handle for this
        val originName = (targetElement as? PsiNameIdentifierOwner)?.name ?: return

        // avoid user typing in template. only suggest that user refactor the name
        TemplateManager.getInstance(project).getActiveTemplate(editor) ?: return

        if (originName.isBlank()) return

        val promptText =
            "$originName is a badname. Please provide 5 better options name for follow code: \n```${targetElement.language.displayName}\n${targetElement.text}\n```\n\n1."

        try {
            doExecuteNameSuggest(promptText, lookupImpl)
        } catch (e: Exception) {
            logger.warn("Error in RenameLookupManagerListener", e)
        }
    }

    private fun doExecuteNameSuggest(promptText: String, lookupImpl: LookupImpl) {
        val stringJob = AutoDevCoroutineScope.scope(project).launch {
            AutoDevStatusService.notifyApplication(AutoDevStatus.InProgress)

            val runJob = currentCoroutineContext().job
            try {
                val stringFlow: Flow<String> = llm.stream(promptText, "", false)
                val sb = StringBuilder()

                stringFlow.cancellable().collect {
                    if (runJob.job.isCancelled) {
                        currentCoroutineContext().job.cancel()
                    }
                    sb.append(it)
                }
                val result = sb.toString()

                extractSuggestionsFromString(result).filter {
                    // since AI could not do well in math, like 5 results, we should filter it
                    it.isNotBlank() && !it.contains(" ")
                }.map {
                    runReadAction {
                        if (!lookupImpl.isLookupDisposed && runJob.isActive && it.isNotBlank()) {
                            val newSuggestion = it.removeSurrounding("`")
                            lookupImpl.addItem(CustomRenameLookupElement(newSuggestion), PrefixMatcher.ALWAYS_TRUE)
                        }
                    }
                }

                runInEdt {
                    if (!lookupImpl.isLookupDisposed) {
                        lookupImpl.isCalculating = false
                        lookupImpl.refreshUi(true, false)
                    }
                }
            } catch (e: Exception) {
                logger.error("Error in RenameLookupManagerListener", e)
                AutoDevStatusService.notifyApplication(AutoDevStatus.Error)
            }

            AutoDevStatusService.notifyApplication(AutoDevStatus.Ready)
        }

        lookupImpl.addLookupListener(object : LookupListener {
            override fun lookupCanceled(event: LookupEvent) {
                AutoDevStatusService.notifyApplication(AutoDevStatus.Ready)
            }
        })

        stringJob.start()
    }

    private fun lookupElement(
        lookupImpl: LookupImpl,
        editor: EditorEx
    ): PsiElement? {
        val startOffset = lookupImpl.lookupOriginalStart
        val psiFile = PsiEditorUtil.getPsiFile(editor)

        var targetElement: PsiElement? = null
        if (startOffset >= 0) {
            targetElement = psiFile.findElementAt(startOffset)
        }

        if (targetElement == null) {
            targetElement = psiFile.findElementAt(editor.caretModel.offset)
        }

        if (targetElement is LeafPsiElement || targetElement is PsiWhiteSpace) {
            targetElement = targetElement.parent
        }

        return targetElement
    }

    private fun extractSuggestionsFromString(result: String) = result.split("\n").map {
        it.replace(Regex("^\\d+\\."), "")
            .trim()
            .removeSurrounding("`")
            // for markdown highlight
            .removeSurrounding("**")
            .removeSuffix("()")
    }
}
