package cc.unitmesh.devti.language.compiler.processor

import cc.unitmesh.devti.command.InsCommand
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.compiler.error.DEVINS_ERROR
import cc.unitmesh.devti.language.psi.DevInTypes
import cc.unitmesh.devti.language.psi.DevInUsed
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType

/**
 * Processor for commands within USED elements
 */
class CommandProcessor(private val commandFactory: InsCommandFactory) {
    
    suspend fun processCommand(
        command: BuiltinCommand,
        used: DevInUsed,
        id: PsiElement?,
        usedText: String,
        originCmdName: String,
        context: CompilerContext
    ): ProcessResult {
        if (command != BuiltinCommand.TOOLCHAIN_COMMAND && !command.requireProps) {
            return executeCommand(command, "", used, usedText, originCmdName, context)
        }
        
        val propElement = id?.nextSibling?.nextSibling
        val isProp = (propElement?.elementType == DevInTypes.COMMAND_PROP)
        
        if (!isProp && command != BuiltinCommand.TOOLCHAIN_COMMAND) {
            context.appendOutput(usedText)
            context.logger.warn("No command prop found: $usedText")
            context.setError(true)
            return ProcessResult(success = false)
        }
        
        val propText = runReadAction { propElement?.text } ?: ""
        return executeCommand(command, propText, used, usedText, originCmdName, context)
    }
    
    private suspend fun executeCommand(
        commandNode: BuiltinCommand,
        prop: String,
        used: DevInUsed,
        fallbackText: String,
        originCmdName: String,
        context: CompilerContext
    ): ProcessResult {
        val command: InsCommand = commandFactory.createCommand(commandNode, prop, used, originCmdName, context)
        
        val execResult = command.execute()
        
        val isSucceed = execResult?.contains(DEVINS_ERROR) == false
        val result = if (isSucceed) {
            val hasReadCodeBlock = commandNode in listOf(
                BuiltinCommand.WRITE,
                BuiltinCommand.EDIT_FILE,
                BuiltinCommand.COMMIT,
                BuiltinCommand.DATABASE,
                BuiltinCommand.SHELL,
                BuiltinCommand.TOOLCHAIN_COMMAND,
            )
            
            if (hasReadCodeBlock) {
                context.skipNextCode = true
            }
            
            execResult
        } else {
            execResult ?: fallbackText
        }
        
        context.appendOutput(result)
        return ProcessResult(success = isSucceed)
    }
}
