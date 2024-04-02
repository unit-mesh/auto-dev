package cc.unitmesh.devti.practise

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.settings.coder.coderSetting
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.*
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking

class RenameLookupManagerListener(val project: Project) : LookupManagerListener {
    private val llm = LlmFactory.instance.create(project)

    override fun activeLookupChanged(oldLookup: Lookup?, newLookup: Lookup?) {
        val lookupImpl = newLookup as? LookupImpl ?: return

        if (!project.coderSetting.state.enableRenameSuggestion) return

        val lookupOriginalStart = lookupImpl.lookupOriginalStart
        val startOffset = if (lookupOriginalStart > -1) lookupOriginalStart else 0
        val psiElement = lookupImpl.psiFile?.findElementAt(startOffset)
        val element = psiElement ?: lookupImpl.psiElement ?: return

        val parentClass = if (element is LeafPsiElement || element is PsiWhiteSpace) {
            element.parent?.javaClass
        } else {
            element.javaClass
        }

        val name = parentClass?.name ?: element.text

        val promptText = """$name is a badname. Please provide 5 better options name for follow code: 

                ```${element.language.displayName}
                ${element.text}
                ```

                1.
                """.trimIndent()


        ApplicationManager.getApplication().invokeLater {
            runBlocking {
                val stringFlow: Flow<String> = llm.stream(
                    promptText,
                    "",
                    false
                )

                val sb = StringBuilder()
                stringFlow.collect {
                    sb.append(it)
                }

                val result = sb.toString()

                // the prompt will be list format, split with \n and remove start number with regex
                val suggestionNames = result.split("\n").map {
                    it.replace(Regex("^\\d+\\."), "")
                        .trim()
                        .removeSurrounding("`")
                }

                suggestionNames.map {
                    lookupImpl.addItem(RenameLookupElement(it), PrefixMatcher.ALWAYS_TRUE)
                }
            }
        }
    }
}

class RenameLookupElement(val name: String) : LookupElement() {
    override fun getLookupString(): String = name
    override fun renderElement(presentation: LookupElementPresentation) {
        presentation.icon = AutoDevIcons.Idea
        super.renderElement(presentation)
    }
}
