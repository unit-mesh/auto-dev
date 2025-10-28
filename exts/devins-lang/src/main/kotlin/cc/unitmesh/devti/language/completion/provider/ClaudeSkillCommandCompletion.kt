package cc.unitmesh.devti.language.completion.provider

import cc.unitmesh.devti.command.dataprovider.ClaudeSkillCommand
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

/**
 * Provides code completion for Claude Skills loaded from project directories or ~/.claude/skills/.
 * 
 * This completion provider:
 * 1. Loads all available Claude Skills from the project and user directories
 * 2. Creates completion items with skill name, description, and icon
 * 3. Automatically triggers completion after "skill"
 * 
 * Example completions:
 * - /skill.pdf - Handle PDF operations
 * - /skill.algorithmic-art - Create generative art pieces
 * - /skill.artifacts-builder - Build interactive artifacts
 */
class ClaudeSkillCommandCompletion : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val project = parameters.originalFile.project ?: return
        
        // Load all Claude Skills from project directories and ~/.claude/skills/
        ClaudeSkillCommand.all(project).forEach { claudeSkill ->
            val lookupElement = createClaudeSkillCompletionCandidate(claudeSkill)
            result.addElement(lookupElement)
        }
    }

    private fun createClaudeSkillCompletionCandidate(command: ClaudeSkillCommand) =
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
            98.0 // Same priority as SpecKit commands
        )
}

