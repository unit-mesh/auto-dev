package cc.unitmesh.devti.language.compiler.processor

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.command.InsCommand
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand.Companion.toolchainProviderName
import cc.unitmesh.devti.language.compiler.exec.*
import cc.unitmesh.devti.language.compiler.exec.process.KillProcessInsCommand
import cc.unitmesh.devti.language.compiler.exec.process.LaunchProcessInsCommand
import cc.unitmesh.devti.language.compiler.exec.process.ListProcessesInsCommand
import cc.unitmesh.devti.language.compiler.exec.process.ReadProcessOutputInsCommand
import cc.unitmesh.devti.language.compiler.exec.process.WriteProcessInputInsCommand
import cc.unitmesh.devti.language.parser.CodeBlockElement
import cc.unitmesh.devti.language.psi.DevInTypes
import cc.unitmesh.devti.language.psi.DevInUsed
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType

/**
 * Factory for creating InsCommand instances
 */
class InsCommandFactory {
    
    private val logger = logger<InsCommandFactory>()
    
    suspend fun createCommand(
        commandNode: BuiltinCommand,
        prop: String,
        used: DevInUsed,
        originCmdName: String,
        context: CompilerContext
    ): InsCommand = when (commandNode) {
        BuiltinCommand.FILE -> FileInsCommand(context.project, prop)
        BuiltinCommand.REV -> RevInsCommand(context.project, prop)
        BuiltinCommand.SYMBOL -> {
            context.result.isLocalCommand = true
            SymbolInsCommand(context.project, prop)
        }
        BuiltinCommand.WRITE -> {
            context.result.isLocalCommand = true
            val devInCode: CodeBlockElement? = lookupNextCode(used)
            if (devInCode == null) {
                PrintInsCommand("/" + commandNode.commandName + ":" + prop)
            } else {
                WriteInsCommand(context.project, prop, devInCode.codeText(), used)
            }
        }
        BuiltinCommand.PATCH -> {
            context.result.isLocalCommand = true
            val devInCode: CodeBlockElement? = lookupNextCode(used)
            if (devInCode == null) {
                PrintInsCommand("/" + commandNode.commandName + ":" + prop)
            } else {
                PatchInsCommand(context.project, prop, devInCode.codeText())
            }
        }
        BuiltinCommand.EDIT_FILE -> {
            context.result.isLocalCommand = true
            val devInCode: CodeBlockElement? = lookupNextCode(used)
            if (devInCode == null) {
                PrintInsCommand("/" + commandNode.commandName + ":" + prop)
            } else {
                EditFileInsCommand(context.project, prop, devInCode.codeText())
            }
        }
        BuiltinCommand.COMMIT -> {
            context.result.isLocalCommand = true
            val devInCode: CodeBlockElement? = lookupNextCode(used)
            if (devInCode == null) {
                PrintInsCommand("/" + commandNode.commandName + ":" + prop)
            } else {
                CommitInsCommand(context.project, devInCode.codeText())
            }
        }
        BuiltinCommand.RUN -> {
            context.result.isLocalCommand = true
            RunInsCommand(context.project, prop)
        }
        BuiltinCommand.SHELL -> {
            context.result.isLocalCommand = true
            val shireCode: String? = lookupNextCode(used)?.codeText()
            ShellInsCommand(context.project, prop, shireCode)
        }
        BuiltinCommand.BROWSE -> {
            context.result.isLocalCommand = true
            BrowseInsCommand(context.project, prop)
        }
        BuiltinCommand.REFACTOR -> {
            context.result.isLocalCommand = true
            val nextTextSegment = lookupNextTextSegment(used)
            RefactorInsCommand(context.project, prop, nextTextSegment)
        }
        BuiltinCommand.DIR -> {
            context.result.isLocalCommand = true
            DirInsCommand(context.project, prop)
        }
        BuiltinCommand.DATABASE -> {
            context.result.isLocalCommand = true
            val shireCode: String? = lookupNextCode(used)?.codeText()
            DatabaseInsCommand(context.project, prop, shireCode)
        }
        BuiltinCommand.STRUCTURE -> {
            context.result.isLocalCommand = true
            StructureInCommand(context.project, prop)
        }
        BuiltinCommand.LOCAL_SEARCH -> {
            context.result.isLocalCommand = true
            val shireCode: String? = lookupNextCode(used)?.codeText()
            LocalSearchInsCommand(context.project, prop, shireCode)
        }
        BuiltinCommand.RIPGREP_SEARCH -> {
            context.result.isLocalCommand = true
            val shireCode: String? = lookupNextCode(used)?.codeText()
            RipgrepSearchInsCommand(context.project, prop, shireCode)
        }
        BuiltinCommand.RELATED -> {
            context.result.isLocalCommand = true
            RelatedSymbolInsCommand(context.project, prop)
        }
        BuiltinCommand.RULE -> {
            context.result.isLocalCommand = true
            RuleInsCommand(context.project, prop)
        }
        BuiltinCommand.USAGE -> {
            context.result.isLocalCommand = true
            UsageInsCommand(context.project, prop)
        }
        BuiltinCommand.OPEN -> {
            context.result.isLocalCommand = true
            OpenInsCommand(context.project, prop)
        }
        BuiltinCommand.LAUNCH_PROCESS -> {
            context.result.isLocalCommand = true
            val shireCode: String? = lookupNextCode(used)?.codeText()
            LaunchProcessInsCommand(context.project, prop, shireCode)
        }
        BuiltinCommand.LIST_PROCESSES -> {
            context.result.isLocalCommand = true
            ListProcessesInsCommand(context.project, prop)
        }
        BuiltinCommand.KILL_PROCESS -> {
            context.result.isLocalCommand = true
            KillProcessInsCommand(context.project, prop)
        }
        BuiltinCommand.READ_PROCESS_OUTPUT -> {
            context.result.isLocalCommand = true
            ReadProcessOutputInsCommand(context.project, prop)
        }
        BuiltinCommand.WRITE_PROCESS_INPUT -> {
            context.result.isLocalCommand = true
            val shireCode: String? = lookupNextCode(used)?.codeText()
            WriteProcessInputInsCommand(context.project, prop, shireCode)
        }
        BuiltinCommand.TOOLCHAIN_COMMAND -> {
            context.result.isLocalCommand = true
            createToolchainCommand(used, prop, originCmdName, commandNode, context)
        }
        else -> PrintInsCommand("/" + commandNode.commandName + ":" + prop)
    }
    
    private suspend fun createToolchainCommand(
        used: DevInUsed,
        prop: String,
        originCmdName: String,
        commandNode: BuiltinCommand,
        context: CompilerContext
    ): InsCommand {
        return try {
            val providerName = toolchainProviderName(originCmdName)
            val provider = ToolchainFunctionProvider.lookup(providerName)
            if (provider != null) {
                executeExtensionFunction(used, prop, provider, context)
            } else {
                var cmd = PrintInsCommand("/" + commandNode.commandName + ":" + prop)
                ToolchainFunctionProvider.all().forEach {
                    if (it.funcNames().contains(originCmdName)) {
                        cmd = executeExtensionFunction(used, prop, it, context)
                    }
                }
                cmd
            }
        } catch (e: Exception) {
            PrintInsCommand("/" + commandNode.commandName + ":" + prop)
        }
    }
    
    private suspend fun executeExtensionFunction(
        used: DevInUsed,
        prop: String,
        provider: ToolchainFunctionProvider,
        context: CompilerContext
    ): PrintInsCommand {
        val codeContent: String? = runReadAction { lookupNextCode(used)?.text }
        val args = if (codeContent != null) {
            val code = CodeFence.parse(codeContent).text
            listOf(code)
        } else {
            listOf()
        }
        
        val result = try {
            val cmd = runReadAction { used.text.removePrefix("/") }
            provider.execute(context.project, prop, args, emptyMap(), cmd).toString()
        } catch (e: Exception) {
            logger.warn(e)
            val text = runReadAction { used.text }
            val nextCode = lookupNextCode(used)?.codeText()
            val error = "Error executing toolchain function: $text:$prop, $nextCode\n" +
                    "Error: ${e.message}\n" +
                    "Please check the command and try again."
            AutoDevNotifications.notify(context.project, error)
            error
        }
        
        return PrintInsCommand(result)
    }
    
    private fun lookupNextCode(used: DevInUsed): CodeBlockElement? {
        var next: PsiElement? = used
        while (true) {
            next = next?.nextSibling
            if (next == null) {
                return null
            }
            
            if (next.elementType == DevInTypes.CODE) {
                return next as CodeBlockElement
            }
        }
    }
    
    private fun lookupNextTextSegment(used: DevInUsed): String {
        val textSegment: StringBuilder = StringBuilder()
        var next: PsiElement? = used
        while (true) {
            next = next?.nextSibling
            if (next == null) {
                break
            }
            
            if (next.elementType == DevInTypes.TEXT_SEGMENT) {
                textSegment.append(next.text)
                break
            }
        }
        
        return textSegment.toString()
    }
}
