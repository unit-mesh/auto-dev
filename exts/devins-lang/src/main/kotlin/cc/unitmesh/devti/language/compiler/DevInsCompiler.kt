package cc.unitmesh.devti.language.compiler

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.agent.custom.model.CustomAgentConfig
import cc.unitmesh.devti.custom.compile.VariableTemplateCompiler
import cc.unitmesh.devti.command.InsCommand
import cc.unitmesh.devti.language.compiler.error.DEVINS_ERROR
import cc.unitmesh.devti.language.compiler.exec.*
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand.Companion.toolchainProviderName
import cc.unitmesh.devti.command.dataprovider.CustomCommand
import cc.unitmesh.devti.command.dataprovider.ToolHubVariable
import cc.unitmesh.devti.language.parser.CodeBlockElement
import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.language.psi.DevInTypes
import cc.unitmesh.devti.language.psi.DevInUsed
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val CACHED_COMPILE_RESULT = mutableMapOf<String, DevInsCompiledResult>()

class DevInsCompiler(
    private val myProject: Project,
    private val file: DevInFile,
    private val editor: Editor? = null,
    private val element: PsiElement? = null
) {
    private var skipNextCode: Boolean = false
    private val logger = logger<DevInsCompiler>()
    private val result = DevInsCompiledResult()
    private val output: StringBuilder = StringBuilder()

    /**
     * Todo: build AST tree, then compile
     */
    suspend fun compile(): DevInsCompiledResult = withContext(Dispatchers.IO) {
        result.input = runReadAction { file.text }
        val children = runReadAction { file.children }
        children.forEach {
            val text = runReadAction { it.text }
            when (it.elementType) {
                DevInTypes.TEXT_SEGMENT -> output.append(text)
                DevInTypes.NEWLINE -> output.append("\n")
                DevInTypes.CODE -> {
                    if (skipNextCode) {
                        skipNextCode = false
                        return@forEach
                    }

                    output.append(text)
                }

                DevInTypes.USED -> processUsed(it as DevInUsed)
                DevInTypes.COMMENTS -> {
                    if (text.startsWith("[flow]:")) {
                        val fileName = text.substringAfter("[flow]:").trim()
                        val content =
                            myProject.guessProjectDir()?.findFileByRelativePath(fileName)?.let { virtualFile ->
                                virtualFile.inputStream.bufferedReader().use { reader -> reader.readText() }
                            }

                        if (content != null) {
                            val devInFile = DevInFile.fromString(myProject, content)
                            result.nextJob = devInFile
                        }
                    }
                }

                else -> {
                    output.append(text)
                    logger.warn("Unknown element type: ${it.elementType}")
                }
            }
        }

        result.output = output.toString()

        CACHED_COMPILE_RESULT[file.name] = result
        return@withContext result
    }

    suspend fun processUsed(used: DevInUsed) {
        val firstChild = used.firstChild
        val id = firstChild.nextSibling

        val usedText = runReadAction { used.text }
        when (firstChild.elementType) {
            DevInTypes.COMMAND_START -> {
                val originCmdName = id?.text ?: ""
                val command = BuiltinCommand.fromString(originCmdName)
                if (command == null) {
                    AutoDevNotifications.notify(myProject, "Cannot find command: $originCmdName")
                    CustomCommand.fromString(myProject, originCmdName)?.let { cmd ->
                        DevInFile.fromString(myProject, cmd.content).let { file ->
                            DevInsCompiler(myProject, file).compile().let {
                                output.append(it.output)
                                result.hasError = it.hasError
                            }
                        }

                        return
                    }

                    output.append(usedText)
                    logger.warn("Unknown command: $originCmdName")
                    result.hasError = true
                    return
                }

                if (command != BuiltinCommand.TOOLCHAIN_COMMAND && !command.requireProps) {
                    processingCommand(command, "", used, fallbackText = usedText, originCmdName)
                    return
                }

                val propElement = id.nextSibling?.nextSibling
                val isProp = (propElement.elementType == DevInTypes.COMMAND_PROP)
                if (!isProp && command != BuiltinCommand.TOOLCHAIN_COMMAND) {
                    output.append(usedText)
                    logger.warn("No command prop found: $usedText")
                    result.hasError = true
                    return
                }

                val propText = runReadAction { propElement?.text } ?: ""
                processingCommand(command, propText, used, fallbackText = usedText, originCmdName)
            }

            DevInTypes.AGENT_START -> {
                val agentId = id?.text
                val configs = CustomAgentConfig.loadFromProject(myProject).filter {
                    it.name == agentId
                }

                if (configs.isNotEmpty()) {
                    result.executeAgent = configs.first()
                }
            }

            DevInTypes.VARIABLE_START -> {
                val variableId = id?.text
                val variable = ToolHubVariable.lookup(myProject, variableId)
                if (variable.isNotEmpty()) {
                    output.append(variable.joinToString("\n") { it })
                    return
                }

                if (editor == null || element == null) {
                    output.append("$DEVINS_ERROR No context editor found for variable: $usedText")
                    result.hasError = true
                    return
                }

                val file = element.containingFile
                VariableTemplateCompiler(file.language, file, element, editor).compile(usedText).let {
                    output.append(it)
                }
            }

            else -> {
                logger.warn("Unknown [cc.unitmesh.devti.language.psi.DevInUsed] type: ${firstChild.elementType}")
                output.append(usedText)
            }
        }
    }

    private suspend fun processingCommand(
        commandNode: BuiltinCommand,
        prop: String,
        used: DevInUsed,
        fallbackText: String,
        originCmdName: String
    ) {
        val command: InsCommand = toInsCommand(commandNode, prop, used, originCmdName)

        val execResult = command.execute()

        val isSucceed = execResult?.contains(DEVINS_ERROR) == false
        val result = if (isSucceed) {
            val hasReadCodeBlock = commandNode in listOf(
                BuiltinCommand.WRITE,
                BuiltinCommand.PATCH,
                BuiltinCommand.COMMIT,
                BuiltinCommand.DATABASE,
                BuiltinCommand.SHELL,
                /// since we cannot control toolchain function's output, we should not skip next code block
                BuiltinCommand.TOOLCHAIN_COMMAND,
            )

            if (hasReadCodeBlock) {
                skipNextCode = true
            }

            execResult
        } else {
            execResult ?: fallbackText
        }

        output.append(result)
    }

    suspend fun toInsCommand(
        commandNode: BuiltinCommand,
        prop: String,
        used: DevInUsed,
        originCmdName: String
    ): InsCommand = when (commandNode) {
        BuiltinCommand.FILE -> {
            FileInsCommand(myProject, prop)
        }

        BuiltinCommand.REV -> {
            RevInsCommand(myProject, prop)
        }

        BuiltinCommand.SYMBOL -> {
            result.isLocalCommand = true
            SymbolInsCommand(myProject, prop)
        }

        BuiltinCommand.WRITE -> {
            result.isLocalCommand = true
            val devInCode: CodeBlockElement? = lookupNextCode(used)
            if (devInCode == null) {
                PrintInsCommand("/" + commandNode.commandName + ":" + prop)
            } else {
                WriteInsCommand(myProject, prop, devInCode.codeText(), used)
            }
        }

        BuiltinCommand.PATCH -> {
            result.isLocalCommand = true
            val devInCode: CodeBlockElement? = lookupNextCode(used)
            if (devInCode == null) {
                PrintInsCommand("/" + commandNode.commandName + ":" + prop)
            } else {
                PatchInsCommand(myProject, prop, devInCode.codeText())
            }
        }

        BuiltinCommand.COMMIT -> {
            result.isLocalCommand = true
            val devInCode: CodeBlockElement? = lookupNextCode(used)
            if (devInCode == null) {
                PrintInsCommand("/" + commandNode.commandName + ":" + prop)
            } else {
                CommitInsCommand(myProject, devInCode.codeText())
            }
        }

        BuiltinCommand.RUN -> {
            result.isLocalCommand = true
            RunInsCommand(myProject, prop)
        }

        BuiltinCommand.SHELL -> {
            result.isLocalCommand = true
            val shireCode: String? = lookupNextCode(used)?.codeText()
            ShellInsCommand(myProject, prop, shireCode)
        }

        BuiltinCommand.BROWSE -> {
            result.isLocalCommand = true
            BrowseInsCommand(myProject, prop)
        }

        BuiltinCommand.REFACTOR -> {
            result.isLocalCommand = true
            val nextTextSegment = lookupNextTextSegment(used)
            RefactorInsCommand(myProject, prop, nextTextSegment)
        }

        BuiltinCommand.DIR -> {
            result.isLocalCommand = true
            DirInsCommand(myProject, prop)
        }

        BuiltinCommand.DATABASE -> {
            result.isLocalCommand = true
            val shireCode: String? = lookupNextCode(used)?.codeText()
            DatabaseInsCommand(myProject, prop, shireCode)
        }

        BuiltinCommand.STRUCTURE -> {
            result.isLocalCommand = true
            StructureInCommand(myProject, prop)
        }

        BuiltinCommand.LOCAL_SEARCH -> {
            result.isLocalCommand = true
            val shireCode: String? = lookupNextCode(used)?.codeText()
            LocalSearchInsCommand(myProject, prop, shireCode)
        }

        BuiltinCommand.RIPGREP_SEARCH -> {
            result.isLocalCommand = true
            val shireCode: String? = lookupNextCode(used)?.codeText()
            RipgrepSearchInsCommand(myProject, prop, shireCode)
        }

        BuiltinCommand.RELATED -> {
            result.isLocalCommand = true
            RelatedSymbolInsCommand(myProject, prop)
        }

        BuiltinCommand.RULE -> {
            result.isLocalCommand = true
            RuleInsCommand(myProject, prop)
        }

        BuiltinCommand.USAGE -> {
            result.isLocalCommand = true
            UsageInsCommand(myProject, prop)
        }

        BuiltinCommand.OPEN -> {
            result.isLocalCommand = true
            OpenInsCommand(myProject, prop)
        }

        BuiltinCommand.TOOLCHAIN_COMMAND -> {
            result.isLocalCommand = true
            try {
                val providerName = toolchainProviderName(originCmdName)
                val provider = ToolchainFunctionProvider.lookup(providerName)
                if (provider != null) {
                    executeExtensionFunction(used, prop, provider)
                } else {
                    var cmd = PrintInsCommand("/" + commandNode.commandName + ":" + prop)
                    ToolchainFunctionProvider.all().forEach {
                        if (it.funcNames().contains(originCmdName)) {
                            cmd = executeExtensionFunction(used, prop, it)
                        }
                    }

                    cmd
                }
            } catch (e: Exception) {
                PrintInsCommand("/" + commandNode.commandName + ":" + prop)
            }
        }

        else -> {
            PrintInsCommand("/" + commandNode.commandName + ":" + prop)
        }
    }

    private suspend fun executeExtensionFunction(
        used: DevInUsed,
        prop: String,
        provider: ToolchainFunctionProvider
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
            provider.execute(myProject, prop, args, emptyMap(), cmd).toString()
        } catch (e: Exception) {
            logger<DevInsCompiler>().warn(e)
            val text = runReadAction { used.text }
            val nextCode = lookupNextCode(used)?.codeText()
            val error = "Error executing toolchain function: $text:$prop, $nextCode\n" +
                    "Error: ${e.message}\n" +
                    "Please check the command and try again."
            AutoDevNotifications.notify(myProject, error)
            error
        }

        return PrintInsCommand(result)
    }

    private fun lookupNextCode(used: DevInUsed): CodeBlockElement? {
        val devInCode: CodeBlockElement?
        var next: PsiElement? = used
        while (true) {
            next = next?.nextSibling
            if (next == null) {
                devInCode = null
                break
            }

            if (next.elementType == DevInTypes.CODE) {
                devInCode = next as CodeBlockElement
                break
            }
        }

        return devInCode
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

    companion object {
        suspend fun transpileCommand(file: DevInFile): List<BuiltinCommand> {
            val children = runReadAction { file.children }
            return children.mapNotNull { it ->
                when (it.elementType) {
                    DevInTypes.USED -> {
                        val used = it as DevInUsed
                        val firstChild = used.firstChild
                        val id = firstChild.nextSibling

                        return@mapNotNull when (firstChild.elementType) {
                            DevInTypes.COMMAND_START -> {
                                val originCmdName = id?.text ?: ""
                                val command = BuiltinCommand.fromString(originCmdName)
                                if (command == null) {
                                    CustomCommand.fromString(file.project, originCmdName) ?: return@mapNotNull null
                                }

                                command
                            }

                            else -> null
                        }
                    }

                    else -> null
                }
            }
        }
    }
}


