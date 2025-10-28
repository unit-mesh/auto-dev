package cc.unitmesh.devti.language.compiler.processor

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.agent.custom.model.CustomAgentConfig
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.command.dataprovider.CustomCommand
import cc.unitmesh.devti.command.dataprovider.SpecKitCommand
import cc.unitmesh.devti.language.compiler.DevInsCompiler
import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.language.psi.DevInTypes
import cc.unitmesh.devti.language.psi.DevInUsed
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType

/**
 * Processor for USED elements (commands, agents, variables)
 */
class UsedProcessor(
    private val commandProcessor: CommandProcessor,
    private val variableProcessor: VariableProcessor
) : DevInElementProcessor {
    
    override suspend fun process(element: PsiElement, context: CompilerContext): ProcessResult {
        val used = element as DevInUsed
        val firstChild = used.firstChild
        val id = firstChild.nextSibling
        val usedText = runReadAction { used.text }
        
        when (firstChild.elementType) {
            DevInTypes.COMMAND_START -> {
                return processCommand(used, id, usedText, context)
            }
            
            DevInTypes.AGENT_START -> {
                return processAgent(id, context)
            }
            
            DevInTypes.VARIABLE_START -> {
                return processVariable(firstChild, usedText, context)
            }
            
            else -> {
                context.logger.warn("Unknown DevInUsed type: ${firstChild.elementType}")
                context.appendOutput(usedText)
                return ProcessResult(success = true)
            }
        }
    }
    
    private suspend fun processCommand(
        used: DevInUsed, 
        id: PsiElement?, 
        usedText: String, 
        context: CompilerContext
    ): ProcessResult {
        val originCmdName = id?.text ?: ""
        val command = BuiltinCommand.fromString(originCmdName)
        
        if (command == null) {
            AutoDevNotifications.notify(context.project, "Cannot find command: $originCmdName")
            // Try spec kit
            SpecKitCommand.fromFullName(context.project, originCmdName)?.let { cmd ->
                cmd.executeWithCompiler(context.project, used.text).let {
                    context.appendOutput(it)
                }
                return ProcessResult(success = true)
            }

            // Try custom command
            CustomCommand.fromString(context.project, originCmdName)?.let { cmd ->
                DevInFile.fromString(context.project, cmd.content).let { file ->
                    DevInsCompiler(context.project, file).compile().let {
                        context.appendOutput(it.output)
                        context.setError(it.hasError)
                    }
                }
                return ProcessResult(success = true)
            }
            
            context.appendOutput(usedText)
            context.logger.warn("Unknown command: $originCmdName")
            context.setError(true)
            return ProcessResult(success = false)
        }
        
        return commandProcessor.processCommand(command, used, id, usedText, originCmdName, context)
    }
    
    private fun processAgent(id: PsiElement?, context: CompilerContext): ProcessResult {
        val agentId = id?.text
        val configs = CustomAgentConfig.loadFromProject(context.project).filter {
            it.name == agentId
        }
        
        if (configs.isNotEmpty()) {
            context.result.executeAgent = configs.first()
        }
        
        return ProcessResult(success = true)
    }
    
    private fun processVariable(
        firstChild: PsiElement, 
        usedText: String, 
        context: CompilerContext
    ): ProcessResult {
        val result = variableProcessor.processVariable(firstChild, context)
        if (!context.isError()) {
            context.appendOutput(usedText)
        }
        return result
    }
    
    override fun canProcess(element: PsiElement): Boolean {
        return element.elementType == DevInTypes.USED
    }
}
