package cc.unitmesh.devti.language.completion.provider

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.agent.tool.AgentTool
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class ToolchainCommandCompletion : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        try {
            CompletableFuture.supplyAsync {
                runBlocking {
                    BuiltinCommand.allToolchains(parameters.originalFile.project).forEach {
                        val lookupElement = createCommandCompletionCandidate(it)
                        result.addElement(lookupElement)
                    }
                }
                null
            }.get(10, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            // Handle timeout exception
        } catch (e: Exception) {
            // Handle other exceptions
        }
    }

    private fun createCommandCompletionCandidate(tool: AgentTool): LookupElement {
        if (!tool.isMcp) {
            val element = LookupElementBuilder.create(tool.name).withIcon(AutoDevIcons.TOOLCHAIN)
            return PrioritizedLookupElement.withPriority(element, 98.0)
        }

        val element = LookupElementBuilder.create(tool.name).withIcon(AutoDevIcons.MCP)
            .withTailText(getText(tool))
            .withInsertHandler { context, _ ->
                context.editor.caretModel.moveToOffset(context.tailOffset)
                context.editor.document.insertString(context.tailOffset, "\n```json\n${tool.completion}\n```")
            }

        return PrioritizedLookupElement.withPriority(element, 97.0)
    }

    private fun getText(tool: AgentTool): String {
        return if (tool.mcpGroup.isNotEmpty()) {
            " ${tool.mcpGroup}: ${tool.description}"
        } else {
            tool.description
        }
    }
}
