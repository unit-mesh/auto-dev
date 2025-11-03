package cc.unitmesh.devins.compiler.processor

import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.devins.ast.DevInsCommandNode
import cc.unitmesh.devins.ast.DevInsNode
import cc.unitmesh.devins.command.SpecKitCommand
import cc.unitmesh.devins.command.SpecKitTemplateCompiler
import cc.unitmesh.devins.compiler.context.CompilerContext

class CommandProcessor : BaseDevInsNodeProcessor() {
    private var specKitCommands: List<SpecKitCommand>? = null
    
    override suspend fun process(node: DevInsNode, context: CompilerContext): ProcessResult {
        logProcessing(node, context)
        
        when (node) {
            is DevInsCommandNode -> {
                return processCommand(node, context)
            }
            
            else -> {
                context.logger.warn("[$name] Unexpected node type: ${node.nodeType}")
                return ProcessResult.failure("Cannot process node type: ${node.nodeType}")
            }
        }
    }
    
    private suspend fun processCommand(node: DevInsCommandNode, context: CompilerContext): ProcessResult {
        val commandName = node.name
        val arguments = node.arguments
        
        context.logger.info("[$name] Processing command: $commandName with ${arguments.size} arguments")
        
        // 更新统计信息
        context.result.statistics.commandCount++
        
        // 获取命令参数文本
        val argumentsText = arguments.joinToString(" ") { getNodeText(it) }
        
        // 检查是否为 SpecKit 命令
        if (commandName.startsWith("speckit.")) {
            return processSpecKitCommand(commandName, argumentsText, context)
        }
        
        // 根据命令类型进行处理
        return when (commandName.lowercase()) {
            "file", ToolType.ReadFile.name -> processFileCommand(commandName, argumentsText, context)
            "symbol" -> processSymbolCommand(commandName, argumentsText, context)
            "write", ToolType.WriteFile.name -> processWriteCommand(commandName, argumentsText, context)
            "run" -> processRunCommand(commandName, argumentsText, context)
            "shell", ToolType.Shell.name -> processShellCommand(commandName, argumentsText, context)
            "search", ToolType.Grep.name, ToolType.Glob.name -> processSearchCommand(commandName, argumentsText, context)
            "patch" -> processPatchCommand(commandName, argumentsText, context)
            "browse" -> processBrowseCommand(commandName, argumentsText, context)
            else -> processUnknownCommand(commandName, argumentsText, context)
        }
    }
    
    private suspend fun processSpecKitCommand(
        commandName: String,
        arguments: String,
        context: CompilerContext
    ): ProcessResult {
        context.logger.info("[$name] Processing SpecKit command: $commandName")
        
        if (specKitCommands == null) {
            specKitCommands = SpecKitCommand.loadAll(context.fileSystem)
            specKitCommands?.forEach { cmd ->
                println("   - ${cmd.fullCommandName}: ${cmd.description}")
            }
            context.logger.info("[$name] Loaded ${specKitCommands?.size ?: 0} SpecKit commands")
        }
        
        val command = SpecKitCommand.findByFullName(specKitCommands ?: emptyList(), commandName)
        
        if (command == null) {
            context.logger.warn("[$name] SpecKit command not found: $commandName")
            return ProcessResult.failure("SpecKit command not found: $commandName")
        }
        
        val compiler = SpecKitTemplateCompiler(
            fileSystem = context.fileSystem,
            template = command.template,
            command = commandName,
            input = arguments
        )
        
        val output = compiler.compile()
        context.appendOutput(output)
        
        return ProcessResult.success(
            output = output,
            metadata = mapOf(
                "command" to commandName,
                "subcommand" to command.subcommand,
                "description" to command.description,
                "isSpecKit" to true
            )
        )
    }
    
    private suspend fun processFileCommand(
        commandName: String, 
        arguments: String, 
        context: CompilerContext
    ): ProcessResult {
        context.logger.info("[$name] Processing file command with arguments: $arguments")
        
        // 标记为本地命令
        context.result.isLocalCommand = true
        
        // 生成文件命令的模板输出
        val output = "{{FILE_CONTENT:$arguments}}"
        context.appendOutput(output)
        
        return ProcessResult.success(
            output = output,
            metadata = mapOf(
                "command" to commandName,
                "filePath" to arguments,
                "isLocal" to true
            )
        )
    }
    
    private suspend fun processSymbolCommand(
        commandName: String, 
        arguments: String, 
        context: CompilerContext
    ): ProcessResult {
        context.logger.info("[$name] Processing symbol command with arguments: $arguments")
        
        context.result.isLocalCommand = true
        
        val output = "{{SYMBOL_INFO:$arguments}}"
        context.appendOutput(output)
        
        return ProcessResult.success(
            output = output,
            metadata = mapOf(
                "command" to commandName,
                "symbol" to arguments,
                "isLocal" to true
            )
        )
    }
    
    private suspend fun processWriteCommand(
        commandName: String, 
        arguments: String, 
        context: CompilerContext
    ): ProcessResult {
        context.logger.info("[$name] Processing write command with arguments: $arguments")
        
        val output = "{{WRITE_FILE:$arguments}}"
        context.appendOutput(output)
        
        return ProcessResult.success(
            output = output,
            metadata = mapOf(
                "command" to commandName,
                "target" to arguments
            )
        )
    }
    
    private suspend fun processRunCommand(
        commandName: String, 
        arguments: String, 
        context: CompilerContext
    ): ProcessResult {
        context.logger.info("[$name] Processing run command with arguments: $arguments")
        
        val output = "{{RUN_COMMAND:$arguments}}"
        context.appendOutput(output)
        
        return ProcessResult.success(
            output = output,
            metadata = mapOf(
                "command" to commandName,
                "runCommand" to arguments
            )
        )
    }
    
    private suspend fun processShellCommand(
        commandName: String, 
        arguments: String, 
        context: CompilerContext
    ): ProcessResult {
        context.logger.info("[$name] Processing shell command with arguments: $arguments")
        
        val output = "{{SHELL_EXEC:$arguments}}"
        context.appendOutput(output)
        
        return ProcessResult.success(
            output = output,
            metadata = mapOf(
                "command" to commandName,
                "shellCommand" to arguments
            )
        )
    }
    
    private suspend fun processSearchCommand(
        commandName: String, 
        arguments: String, 
        context: CompilerContext
    ): ProcessResult {
        context.logger.info("[$name] Processing search command with arguments: $arguments")
        
        context.result.isLocalCommand = true
        
        val output = "{{SEARCH_RESULTS:$arguments}}"
        context.appendOutput(output)
        
        return ProcessResult.success(
            output = output,
            metadata = mapOf(
                "command" to commandName,
                "searchQuery" to arguments,
                "isLocal" to true
            )
        )
    }
    
    private suspend fun processPatchCommand(
        commandName: String, 
        arguments: String, 
        context: CompilerContext
    ): ProcessResult {
        context.logger.info("[$name] Processing patch command with arguments: $arguments")
        
        val output = "{{APPLY_PATCH:$arguments}}"
        context.appendOutput(output)
        
        return ProcessResult.success(
            output = output,
            metadata = mapOf(
                "command" to commandName,
                "patchTarget" to arguments
            )
        )
    }
    
    private suspend fun processBrowseCommand(
        commandName: String, 
        arguments: String, 
        context: CompilerContext
    ): ProcessResult {
        context.logger.info("[$name] Processing browse command with arguments: $arguments")
        
        val output = "{{BROWSE_URL:$arguments}}"
        context.appendOutput(output)
        
        return ProcessResult.success(
            output = output,
            metadata = mapOf(
                "command" to commandName,
                "url" to arguments
            )
        )
    }
    
    private suspend fun processUnknownCommand(
        commandName: String, 
        arguments: String, 
        context: CompilerContext
    ): ProcessResult {
        context.logger.warn("[$name] Unknown command: $commandName")
        
        // 对于未知命令，直接输出原始文本
        val output = "/$commandName:$arguments"
        context.appendOutput(output)
        
        return ProcessResult.success(
            output = output,
            metadata = mapOf(
                "command" to commandName,
                "arguments" to arguments,
                "isUnknown" to true
            )
        )
    }
    
    override fun canProcess(node: DevInsNode): Boolean {
        return node is DevInsCommandNode
    }
}
