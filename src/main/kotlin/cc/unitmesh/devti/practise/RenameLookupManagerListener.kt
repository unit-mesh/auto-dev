package cc.unitmesh.devti.practise

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.settings.coder.coderSetting
import com.intellij.codeInsight.TargetElementUtil
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupManagerListener
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiNameIdentifierOwner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking

class RenameLookupManagerListener(val project: Project) : LookupManagerListener {
    private val llm = LlmFactory.instance.create(project)
    private val logger = logger<RenameLookupManagerListener>()

    override fun activeLookupChanged(oldLookup: Lookup?, newLookup: Lookup?) {
        if (!project.coderSetting.state.enableRenameSuggestion) return

        val lookupImpl = newLookup as? LookupImpl ?: return

        val editor = lookupImpl.editor

        // TODO() check is rename processing?

        val firstCaret = editor.caretModel.allCarets.firstOrNull() ?: return
        val targetElementUtil = TargetElementUtil.getInstance()
        val element = targetElementUtil.findTargetElement(editor, targetElementUtil.allAccepted, firstCaret.offset)

        val originName = (element as? PsiNameIdentifierOwner ?: return).name ?: return

        // check length
        if (originName.isBlank()) return

        val promptText =
            "$originName is a badname. Please provide 5 better options name for follow code: \n```${element.language.displayName}\n${element.text}\n```\n\n1."

        ApplicationManager.getApplication().invokeLater {
            runBlocking {
                val stringFlow: Flow<String> = llm.stream(
                    promptText,
                    "",
                    false
                )

                var result = ""
                stringFlow.collect {
                    result += it
                }

                logger.info("result: $result")
                // add last suggestion
                val suggestionNames = parseSuggestion(result)
                addItems(lookupImpl, suggestionNames)

//                lookupImpl.isCalculating = false
//                lookupImpl.refreshUi(true, false)
            }
        }
    }

    private fun addItems(lookupImpl: LookupImpl, suggestionNames: List<String>): String {
        suggestionNames
            .filter { it.isBlank() }
            .map {
                addItem(lookupImpl, it)
            }

        return suggestionNames.last()
    }

    private fun parseSuggestion(result: String) = result.split("\n").map {
        it.replace(Regex("^\\d+\\."), "")
            .trim()
            .removeSurrounding("`")
            .removeSuffix("()")
    }

    private fun addItem(lookupImpl: LookupImpl, it: String) = runReadAction {
        lookupImpl.addItem(RenameLookupElement(it), PrefixMatcher.ALWAYS_TRUE)
    }
}

class RenameLookupElement(val name: String) : LookupElement() {
    override fun getLookupString(): String = name

    override fun handleInsert(context: InsertionContext) {
        val editor = context.editor
        val templateState = TemplateManagerImpl.getTemplateState(editor)

        if (templateState != null && !templateState.isFinished) {
            @Suppress("UnstableApiUsage")
            templateState.update()
            templateState.considerNextTabOnLookupItemSelected(this)
        }
    }

    override fun renderElement(presentation: LookupElementPresentation) {
        presentation.icon = AutoDevIcons.Idea
        super.renderElement(presentation)
    }
}
