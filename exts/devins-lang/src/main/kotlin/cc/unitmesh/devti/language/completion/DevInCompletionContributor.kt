package cc.unitmesh.devti.language.completion

import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.completion.provider.*
import cc.unitmesh.devti.language.psi.DevInFrontMatterEntry
import cc.unitmesh.devti.language.psi.DevInTypes
import cc.unitmesh.devti.language.psi.DevInUsed
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType

class DevInCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(DevInTypes.LANGUAGE_ID), CodeFenceLanguageCompletion())
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(DevInTypes.VARIABLE_ID), VariableCompletionProvider())
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(DevInTypes.VARIABLE_ID), AgentToolOverviewCompletion())
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(DevInTypes.COMMAND_ID), BuiltinCommandCompletion())
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(DevInTypes.AGENT_ID), CustomAgentCompletion())

        extend(CompletionType.BASIC, identifierAfter(DevInTypes.AGENT_START), CustomAgentCompletion())
        extend(CompletionType.BASIC, identifierAfter(DevInTypes.VARIABLE_START), VariableCompletionProvider())
        extend(CompletionType.BASIC, identifierAfter(DevInTypes.VARIABLE_START), AgentToolOverviewCompletion())
        extend(CompletionType.BASIC, identifierAfter(DevInTypes.COMMAND_START), BuiltinCommandCompletion())

//        extend(CompletionType.BASIC, hobbitHoleKey(), HobbitHoleKeyCompletion())
//        extend(CompletionType.BASIC, hobbitHolePattern(), HobbitHoleValueCompletion())
//
//        extend(CompletionType.BASIC, identifierAfter(DevInTypes.PIPE), PostProcessorCompletion())
//
//        extend(CompletionType.BASIC, whenConditionPattern(), WhenConditionCompletionProvider())
//        extend(CompletionType.BASIC, whenConditionFuncPattern(), WhenConditionFunctionCompletionProvider())

        // command completion
        extend(
            CompletionType.BASIC,
            (valuePatterns(
                listOf(
                    BuiltinCommand.FILE,
                    BuiltinCommand.RUN,
                    BuiltinCommand.WRITE,
                    BuiltinCommand.STRUCTURE
                )
            )),
            FileCompletionProvider()
        )

        extend(
            CompletionType.BASIC,
            (valuePatterns(listOf(BuiltinCommand.SYMBOL, BuiltinCommand.RELATED, BuiltinCommand.USAGE))),
            SymbolReferenceLanguageProvider()
        )

        extend(CompletionType.BASIC, (valuePatterns(listOf(BuiltinCommand.DIR))), DirReferenceLanguageProvider())
        extend(CompletionType.BASIC, valuePattern(BuiltinCommand.REV), RevisionReferenceLanguageProvider())
        extend(CompletionType.BASIC, valuePattern(BuiltinCommand.REFACTOR), RefactoringFuncProvider())
        extend(CompletionType.BASIC, valuePattern(BuiltinCommand.DATABASE), DatabaseFuncCompletionProvider())
        extend(CompletionType.BASIC, valuePattern(BuiltinCommand.RULE), RuleCompletionProvider())
    }

    private inline fun <reified I : PsiElement> psiElement() = PlatformPatterns.psiElement(I::class.java)

    private fun baseUsedPattern(): PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement()
            .inside(psiElement<DevInUsed>())

    private fun identifierAfter(type: IElementType): ElementPattern<out PsiElement> =
        PlatformPatterns.psiElement(DevInTypes.IDENTIFIER)
            .afterLeaf(PlatformPatterns.psiElement().withElementType(type))

    private fun valuePattern(cmd: BuiltinCommand): PsiElementPattern.Capture<PsiElement> = valuePattern(cmd.commandName)

    private fun valuePattern(text: String): PsiElementPattern.Capture<PsiElement> =
        baseUsedPattern()
            .withElementType(DevInTypes.COMMAND_PROP)
            .afterLeafSkipping(
                PlatformPatterns.psiElement(DevInTypes.COLON),
                PlatformPatterns.psiElement().withText(text)
            )

    private fun hobbitHolePattern(): ElementPattern<out PsiElement> {
        return PlatformPatterns.psiElement()
            .inside(psiElement<DevInFrontMatterEntry>())
            .afterLeafSkipping(
                PlatformPatterns.psiElement().withElementType(DevInTypes.FRONT_MATTER_KEY),
                PlatformPatterns.psiElement(DevInTypes.COLON)
            )
    }

    private fun whenConditionPattern(): ElementPattern<out PsiElement> {
        return PlatformPatterns.psiElement()
            .inside(psiElement<DevInFrontMatterEntry>())
            .afterLeaf(PlatformPatterns.psiElement().withText("$"))
    }

    private fun whenConditionFuncPattern(): ElementPattern<out PsiElement> {
        return PlatformPatterns.psiElement(DevInTypes.IDENTIFIER)
            .inside(psiElement<DevInFrontMatterEntry>())
            .afterLeafSkipping(
                PlatformPatterns.psiElement(DevInTypes.IDENTIFIER),
                PlatformPatterns.psiElement(DevInTypes.DOT),
            )
    }

    private fun hobbitHoleKey(): PsiElementPattern.Capture<PsiElement> {
        val excludedElements = listOf(
            DevInTypes.COLON,
            DevInTypes.DOT,
            DevInTypes.AGENT_START,
            DevInTypes.VARIABLE_START,
            DevInTypes.COMMAND_START
        ).map { PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(it)) }

        return excludedElements.fold(
            PlatformPatterns.psiElement(DevInTypes.IDENTIFIER)
        ) { pattern, excludedPattern ->
            pattern.andNot(excludedPattern)
        }
    }

    private fun valuePatterns(listOf: List<BuiltinCommand>): ElementPattern<out PsiElement> {
        val patterns = listOf.map { valuePattern(it.commandName) }
        return PlatformPatterns.or(*patterns.toTypedArray())
    }
}
