package cc.unitmesh.devti.language.completion.lang

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.custom.team.InteractionType
import cc.unitmesh.devti.devins.ShireActionLocation
import cc.unitmesh.devti.devins.post.PostProcessor
import cc.unitmesh.devti.language.ast.HobbitHole
import cc.unitmesh.devti.language.middleware.select.SelectElementStrategy
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

class HobbitHoleValueCompletion : CompletionProvider<CompletionParameters>() {
    private val HOBBIT = "hobbit"

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val position = parameters.originalPosition ?: parameters.position
        val psiElement = PsiTreeUtil.prevVisibleLeaf(position)?.let {
            PsiTreeUtil.prevLeaf(it, true)
        } ?: return

        when (psiElement.text) {
            HOBBIT -> {
                hobbitHeroes(result)
            }

            HobbitHole.ACTION_LOCATION -> {
                ShireActionLocation.all().forEach {
                    result.addElement(
                        LookupElementBuilder
                            .create(it.location)
                            .withIcon(AutoDevIcons.AI_COPILOT)
                            .withInsertHandler { context, _ ->
                                context.document.insertString(context.startOffset, " ")
                            }
                            .withTypeText(it.description, true)
                    )
                }
            }

            HobbitHole.INTERACTION -> {
                InteractionType.entries.forEach {
                    result.addElement(
                        LookupElementBuilder
                            .create(it.name)
                            .withIcon(AutoDevIcons.AI_COPILOT)
                            .withInsertHandler { context, _ ->
                                context.document.insertString(context.startOffset, " ")
                            }
                            .withTypeText(it.description, true)
                    )
                }
            }

            HobbitHole.STRATEGY_SELECTION -> {
                SelectElementStrategy.all().forEach {
                    result.addElement(
                        LookupElementBuilder
                            .create(it)
                            .withIcon(AutoDevIcons.AI_COPILOT)
                            .withInsertHandler { context, _ ->
                                context.document.insertString(context.startOffset, " ")
                            }
                    )
                }
            }

            HobbitHole.ON_STREAMING_END -> {
                PostProcessor.allNames().forEach {
                    result.addElement(
                        LookupElementBuilder
                            .create(it)
                            .withIcon(AutoDevIcons.AI_COPILOT)
                            .withInsertHandler { context: InsertionContext, item: LookupElement ->
                                val offset = context.editor.caretModel.offset
                                val startOffset = offset - item.lookupString.length
                                context.document.deleteString(startOffset, offset)
                                // insert value inside `{ }`, for example, if user select $demo, the insert will be `{ $demo }`
                                val value = " { $it  }"
                                context.document.insertString(context.startOffset, value)

                                context.editor.caretModel.moveToOffset(startOffset + value.length - 2)
                            }
                    )
                }
            }
        }
    }

    private fun hobbitHeroes(result: CompletionResultSet) {
        listOf("Frodo", "Sam", "Merry", "Pippin").forEach {
            result.addElement(LookupElementBuilder.create(it))
        }
    }

}