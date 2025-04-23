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
import cc.unitmesh.devti.language.ast.variable.VariableTable
import cc.unitmesh.devti.language.parser.CodeBlockElement
import cc.unitmesh.devti.language.psi.DevInElseClause
import cc.unitmesh.devti.language.psi.DevInElseifClause
import cc.unitmesh.devti.language.psi.DevInExpr
import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.language.psi.DevInFrontMatterHeader
import cc.unitmesh.devti.language.psi.DevInIfClause
import cc.unitmesh.devti.language.psi.DevInIfExpr
import cc.unitmesh.devti.language.psi.DevInTypes
import cc.unitmesh.devti.language.psi.DevInUsed
import cc.unitmesh.devti.language.psi.DevInVelocityBlock
import cc.unitmesh.devti.language.psi.DevInVelocityExpr
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.lang.parser.GeneratedParserUtilBase.DUMMY_BLOCK
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType.WHITE_SPACE
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.LinkedList

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

    private val variableTable = VariableTable()

    /**
     * Todo: build AST tree, then compile
     */
    suspend fun compile(): DevInsCompiledResult = withContext(Dispatchers.IO) {
        result.input = runReadAction { file.text }
        val children = runReadAction { file.children }
        children.forEach { psiElement ->
            val text = runReadAction { psiElement.text }
            when (psiElement.elementType) {
                DevInTypes.TEXT_SEGMENT -> output.append(text)
                DevInTypes.NEWLINE -> output.append("\n")
                DevInTypes.CODE -> {
                    if (skipNextCode) {
                        skipNextCode = false
                        return@forEach
                    }

                    output.append(text)
                }

                DevInTypes.USED -> processUsed(psiElement as DevInUsed)
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

                DevInTypes.FRONTMATTER_START -> {
                    val nextElement = PsiTreeUtil.findChildOfType(
                        psiElement.parent, DevInFrontMatterHeader::class.java
                    )
                    if (nextElement == null) {
                        return@forEach
                    }
                    result.config = runReadAction { HobbitHoleParser.parse(nextElement) }
                }

                DevInTypes.FRONT_MATTER_HEADER -> {
                    result.config = runReadAction { HobbitHoleParser.parse(psiElement as DevInFrontMatterHeader) }
                }

                WHITE_SPACE, DUMMY_BLOCK -> output.append(psiElement.text)
                DevInTypes.VELOCITY_EXPR -> {
                    processVelocityExpr(psiElement as DevInVelocityExpr)
                    logger.info("Velocity expression found: ${psiElement.text}")
                }

                DevInTypes.MARKDOWN_HEADER -> {
                    output.append("#[[${psiElement.text}]]#")
                }

                else -> {
                    output.append(text)
                    logger.warn("Unknown element type: ${psiElement.elementType}")
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
                processVariable(firstChild)
                if (!result.hasError) output.append(usedText)
            }

            else -> {
                logger.warn("Unknown [cc.unitmesh.devti.language.psi.DevInUsed] type: ${firstChild.elementType}")
                output.append(usedText)
            }
        }
    }

    private fun processVelocityExpr(velocityExpr: DevInVelocityExpr) {
        handleNextSiblingForChild(velocityExpr) { next ->
            if (next is DevInIfExpr) {
                handleNextSiblingForChild(next) {
                    when (it) {
                        is DevInIfClause, is DevInElseifClause, is DevInElseClause -> {
                            handleNextSiblingForChild(it) {
                                runBlocking { processIfClause(it) }
                            }
                        }

                        else -> output.append(it.text)
                    }
                }
            } else {
                output.append(next.text)
            }
        }
    }

    private fun handleNextSiblingForChild(element: PsiElement?, handle: (PsiElement) -> Unit) {
        var child: PsiElement? = element?.firstChild
        while (child != null && !result.hasError) {
            handle(child)
            child = child.nextSibling
        }
    }

    suspend fun processIfClause(clauseContent: PsiElement) {
        when (clauseContent) {
            is DevInExpr -> {
                addVariable(clauseContent)
                if (!result.hasError) output.append(clauseContent.text)
            }

            is DevInVelocityBlock -> {
                DevInFile.fromString(myProject, clauseContent.text).let { file ->
                    val compile = DevInsCompiler(myProject, file).compile()
                    compile.let {
                        output.append(it.output)
                        variableTable.addVariable(it.variableTable)
                        result.hasError = it.hasError
                    }
                }

            }

            else -> {
                output.append(clauseContent.text)
            }
        }
    }

    private fun addVariable(psiElement: PsiElement?) {
        if (psiElement == null) return
        val queue = LinkedList<PsiElement>()
        queue.push(psiElement)
        while (!queue.isEmpty() && !result.hasError) {
            val e = queue.pop()
            if (e.firstChild.elementType == DevInTypes.VARIABLE_START) {
                processVariable(e.firstChild)
            } else {
                e.children.forEach {
                    queue.push(it)
                }
            }
        }
    }

    private fun processVariable(variableStart: PsiElement) {
        if (variableStart.elementType != DevInTypes.VARIABLE_START) {
            logger.warn("Illegal type: ${variableStart.elementType}")
            return
        }
        val variableId = variableStart.nextSibling?.text
//        val variables = ToolHubVariable.lookup(myProject, variableId)
//        val file = element.containingFile
//        VariableTemplateCompiler(file.language, file, element, editor).compile(usedText).let {
//            output.append(it)
//        }

        val currentEditor = editor ?: VariableTemplateCompiler.defaultEditor(myProject)
        val currentElement = element ?: VariableTemplateCompiler.defaultElement(myProject, currentEditor)

        if (currentElement == null) {
            output.append("${DEVINS_ERROR} No element found for variable: ${variableStart.text}")
            result.hasError = true
            return
        }

        val lineNo = try {
            runReadAction {
                val containingFile = currentElement.containingFile
                val document: Document? =     PsiDocumentManager.getInstance(variableStart.project).getDocument(containingFile)
                document?.getLineNumber(variableStart.textRange.startOffset) ?: 0
            }
        } catch (e: Exception) {
            0
        }

        variableTable.addVariable(variableId ?: "", VariableTable.VariableType.String, lineNo)
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


