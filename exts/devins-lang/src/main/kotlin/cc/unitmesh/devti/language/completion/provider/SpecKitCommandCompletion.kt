package cc.unitmesh.devti.language.completion.provider

import cc.unitmesh.devti.command.dataprovider.SpecKitCommand
import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

/**
 * Provides code completion for SpecKit commands loaded from .github/prompts/ directory.
 * 
 * This completion provider:
 * 1. Loads all available spec-kit commands from the project
 * 2. Creates completion items with command name, description, and icon
 * 3. Automatically triggers dot completion after "speckit"
 * 
 * Example completions:
 * - /speckit.clarify - Clarify requirements and edge cases
 * - /speckit.specify - Create detailed specifications
 * - /speckit.plan - Generate technical implementation plan
 */
class SpecKitCommandCompletion : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val project = parameters.originalFile.project ?: return
        
        // Load all SpecKit commands from .github/prompts/
        SpecKitCommand.all(project).forEach { specKitCommand ->
            val lookupElement = createSpecKitCompletionCandidate(specKitCommand)
            result.addElement(lookupElement)
        }
    }

    private fun createSpecKitCompletionCandidate(command: SpecKitCommand) =
        PrioritizedLookupElement.withPriority(
            LookupElementBuilder.create(command.fullCommandName)
                .withIcon(command.icon)
                .withTypeText(command.description, true)
                .withPresentableText(command.fullCommandName)
                .withInsertHandler { context, _ ->
                    // Insert a space after the command for arguments
                    context.document.insertString(context.tailOffset, " ")
                    context.editor.caretModel.moveCaretRelatively(1, 0, false, false, false)
                },
            98.0 // Slightly lower priority than built-in commands (99.0)
        )
}

