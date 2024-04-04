package cc.unitmesh.devti.practise

import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.settings.coder.coderSetting
import cc.unitmesh.devti.statusbar.AutoDevStatus
import cc.unitmesh.devti.statusbar.AutoDevStatusService
import cc.unitmesh.devti.util.LLMCoroutineScope
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.*
import com.intellij.codeInsight.lookup.impl.LookupImpl
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RenameLookupManagerListener(val project: Project) : LookupManagerListener {
    private val llm = LlmFactory.instance.create(project)
    private val logger = logger<RenameLookupManagerListener>()

    override fun activeLookupChanged(oldLookup: Lookup?, newLookup: Lookup?) {
        if (!project.coderSetting.state.enableRenameSuggestion) return

        val lookupImpl = newLookup as? LookupImpl ?: return
        val editor = lookupImpl.editor as? EditorEx ?: return

        val targetElement: PsiElement? = lookupElement(lookupImpl, editor)

        // maybe user just typing, we should handle for this
        val element = targetElement ?: return
        val originName = (element as? PsiNameIdentifierOwner)?.name ?: return

        if (originName.isBlank()) return

        val promptText =
            "$originName is a badname. Please provide 5 better options name for follow code: \n```${element.language.displayName}\n${element.text}\n```\n\n1."


        try {
            doExecuteNameSuggest(promptText, lookupImpl)
        } catch (e: Exception) {
            logger.warn("Error in RenameLookupManagerListener", e)
        }
    }

    private fun doExecuteNameSuggest(promptText: String, lookupImpl: LookupImpl) {
        val stringJob = LLMCoroutineScope.scope(project).launch {
            AutoDevStatusService.notifyApplication(AutoDevStatus.InProgress)

            try {
                val stringFlow: Flow<String> = llm.stream(promptText, "", false)
                val sb = StringBuilder()
                stringFlow.collect {
                    sb.append(it)
                }
                val result = sb.toString()
                logger.info("result: $result")
                extractSuggestionsFromString(result).filter { it.isNotBlank() }.map {
                    runReadAction {
                        if (!lookupImpl.isLookupDisposed) {
                            lookupImpl.addItem(CustomRenameLookupElement(it), PrefixMatcher.ALWAYS_TRUE)
                        }
                    }
                }

                runInEdt {
                    lookupImpl.isCalculating = false
                    lookupImpl.refreshUi(true, false)
                }
            } catch (e: Exception) {
                AutoDevStatusService.notifyApplication(AutoDevStatus.Error)
                logger.error("Error in RenameLookupManagerListener", e)
            }

            AutoDevStatusService.notifyApplication(AutoDevStatus.Ready)
        }

        lookupImpl.addLookupListener(object : LookupListener {
            override fun lookupCanceled(event: LookupEvent) {
                AutoDevStatusService.notifyApplication(AutoDevStatus.Ready)
                stringJob.cancel()
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
            .removeSuffix("()")
    }
}
